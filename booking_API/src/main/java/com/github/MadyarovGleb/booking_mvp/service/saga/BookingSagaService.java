package com.github.MadyarovGleb.booking_mvp.service.saga;

import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.BookingSaga;
import com.github.MadyarovGleb.booking_mvp.entity.BookingSaga.SagaState;
import com.github.MadyarovGleb.booking_mvp.entity.BookingSaga.SagaStep;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.repository.BookingSagaRepository;
import com.github.MadyarovGleb.booking_mvp.service.RedisService;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxEventType;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingSagaService {

    private static final Logger logger = LoggerFactory.getLogger(BookingSagaService.class);

    private final BookingSagaRepository bookingSagaRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityEngineClient availability;
    private final RedisService redisService;
    private final OutboxService outboxService;
    private final TransactionTemplate txTemplate;

    @Value("${app.saga.max-active-bookings-per-user:5}")
    private int maxActiveBookingsPerUser;

    @Value("${app.saga.processor.batch-size:50}")
    private int batchSize;

    @Value("${app.saga.max-retries:5}")
    private int maxRetries;

    @Value("${app.saga.retry-base-delay-ms:1000}")
    private long retryBaseDelayMs;

    public BookingSagaService(
            BookingSagaRepository bookingSagaRepository,
            BookingRepository bookingRepository,
            AvailabilityEngineClient availability,
            RedisService redisService,
            OutboxService outboxService,
            PlatformTransactionManager transactionManager
    ) {
        this.bookingSagaRepository = bookingSagaRepository;
        this.bookingRepository = bookingRepository;
        this.availability = availability;
        this.redisService = redisService;
        this.outboxService = outboxService;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    public void startSaga(Booking booking) {
        BookingSaga saga = BookingSaga.builder()
                .bookingId(booking.getId())
                .state(SagaState.PENDING)
                .currentStep(SagaStep.LIMITS)
                .retryCount(0)
                .maxRetries(maxRetries)
                .nextRetryAt(OffsetDateTime.now())
                .traceId(MDC.get("trace_id"))
                .build();

        BookingSaga created = bookingSagaRepository.save(saga);
        MDC.put("saga_id", created.getId().toString());

        outboxService.addEvent(OutboxEventType.BOOKING_SAGA_STARTED, basePayload(booking, created));
        logger.info("Booking saga started");
    }

    @Scheduled(fixedDelayString = "${app.saga.processor.delay-ms:2000}")
    public void processDueSagas() {
        List<UUID> sagaIds = txTemplate.execute(status -> claimDueSagas());
        if (sagaIds == null || sagaIds.isEmpty()) {
            return;
        }
        for (UUID sagaId : sagaIds) {
            txTemplate.executeWithoutResult(status -> processSingleSaga(sagaId));
        }
    }

    private List<UUID> claimDueSagas() {
        List<BookingSaga> due = bookingSagaRepository.lockDueForProcessing(batchSize);
        for (BookingSaga saga : due) {
            saga.setState(SagaState.PROCESSING);
        }
        return due.stream().map(BookingSaga::getId).toList();
    }

    private void processSingleSaga(UUID sagaId) {
        BookingSaga saga = bookingSagaRepository.findById(sagaId).orElse(null);
        if (saga == null) {
            return;
        }

        Booking booking = bookingRepository.findById(saga.getBookingId()).orElse(null);
        bindMdc(saga, booking);

        try {
            if (booking == null) {
                logger.warn("Saga booking reference missing, compensating saga");
                markCompensated(saga, null, "booking_not_found");
                return;
            }

            if (saga.getState() == SagaState.CONFIRMED || saga.getState() == SagaState.COMPENSATED) {
                return;
            }

            if (booking.getStatus() == Booking.BookingStatus.cancelled) {
                markCompensated(saga, booking, "booking_cancelled");
                return;
            }
            if (booking.getStatus() == Booking.BookingStatus.confirmed) {
                markConfirmed(saga, booking);
                return;
            }
            if (booking.getStatus() == Booking.BookingStatus.rejected) {
                markCompensated(saga, booking, "booking_rejected");
                return;
            }

            if (saga.getCurrentStep() == SagaStep.LIMITS) {
                if (isUserLimitExceeded(booking)) {
                    logger.warn("Saga limit validation failed");
                    compensateBooking(saga, booking, "user_booking_limit_exceeded");
                    return;
                }
                saga.setCurrentStep(SagaStep.AVAILABILITY);
                saga.setState(SagaState.PROCESSING);
                saga.setLastError(null);
                saga.setNextRetryAt(OffsetDateTime.now());
                logger.info("Saga limits step completed");
            }

            if (saga.getCurrentStep() == SagaStep.AVAILABILITY) {
                ValidationResult result = availability.validate(
                        booking.getRoomId(),
                        booking.getStartsAt(),
                        booking.getEndsAt(),
                        booking.getId()
                );

                List<BookingConflict> conflicts = result.getConflicts().stream()
                        .filter(c -> c.getBookingId() != null && !c.getBookingId().isEmpty())
                        .toList();

                if (!conflicts.isEmpty() || !result.isValid()) {
                    logger.warn("Saga availability validation failed conflicts_count={}", conflicts.size());
                    compensateBooking(saga, booking, "availability_conflict");
                    return;
                }

                booking.setStatus(Booking.BookingStatus.confirmed);
                bookingRepository.save(booking);
                invalidateCache(booking);
                markConfirmed(saga, booking);
                outboxService.addEvent(OutboxEventType.BOOKING_CONFIRMED, basePayload(booking, saga));
                logger.info("Saga completed successfully booking confirmed");
            }
        } catch (Exception e) {
            onStepFailure(saga, booking, e);
        } finally {
            clearMdc();
        }
    }

    private boolean isUserLimitExceeded(Booking booking) {
        long activeCount = bookingRepository.countActiveByUserId(
                booking.getUserId(),
                OffsetDateTime.now()
        );
        return activeCount > maxActiveBookingsPerUser;
    }

    private void onStepFailure(BookingSaga saga, Booking booking, Exception e) {
        int nextRetry = saga.getRetryCount() + 1;
        saga.setRetryCount(nextRetry);
        saga.setLastError(trim(e.getMessage(), 1000));

        if (nextRetry >= saga.getMaxRetries()) {
            logger.error("Saga retries exhausted, compensating booking reason={}", e.getClass().getSimpleName());
            compensateBooking(saga, booking, "external_step_failed");
            return;
        }

        long multiplier = 1L << Math.min(nextRetry, 10);
        saga.setState(SagaState.PENDING);
        saga.setNextRetryAt(OffsetDateTime.now().plus(Duration.ofMillis(retryBaseDelayMs * multiplier)));
        logger.warn("Saga step failed, retry scheduled retries={}/{} next_retry_at={}",
                nextRetry, saga.getMaxRetries(), saga.getNextRetryAt());
    }

    private void compensateBooking(BookingSaga saga, Booking booking, String reason) {
        if (booking != null && booking.getStatus() != Booking.BookingStatus.cancelled) {
            booking.setStatus(Booking.BookingStatus.rejected);
            bookingRepository.save(booking);
            invalidateCache(booking);
        }

        markCompensated(saga, booking, reason);
        if (booking != null) {
            Map<String, Object> payload = basePayload(booking, saga);
            payload.put("reason", reason);
            outboxService.addEvent(OutboxEventType.BOOKING_REJECTED, payload);
        }
        logger.warn("Saga compensated booking reason={}", reason);
    }

    private void markConfirmed(BookingSaga saga, Booking booking) {
        saga.setState(SagaState.CONFIRMED);
        saga.setCurrentStep(SagaStep.COMPLETED);
        saga.setLastError(null);
        saga.setNextRetryAt(OffsetDateTime.now());
        if (booking != null) {
            saga.setBookingId(booking.getId());
        }
    }

    private void markCompensated(BookingSaga saga, Booking booking, String reason) {
        saga.setState(SagaState.COMPENSATED);
        saga.setCurrentStep(SagaStep.COMPENSATED);
        saga.setLastError(reason);
        saga.setNextRetryAt(OffsetDateTime.now());
        if (booking != null) {
            saga.setBookingId(booking.getId());
        }
    }

    private Map<String, Object> basePayload(Booking booking, BookingSaga saga) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("booking_id", booking.getId().toString());
        payload.put("user_id", booking.getUserId().toString());
        payload.put("room_id", booking.getRoomId().toString());
        payload.put("starts_at", booking.getStartsAt().toString());
        payload.put("ends_at", booking.getEndsAt().toString());
        payload.put("status", booking.getStatus().name().toLowerCase());
        if (saga != null && saga.getId() != null) {
            payload.put("saga_id", saga.getId().toString());
        }
        return payload;
    }

    private void invalidateCache(Booking booking) {
        redisService.delete(String.format("availability:%s:%s", booking.getRoomId(), booking.getStartsAt().toLocalDate()));
        redisService.deletePattern("rooms:search:*");
    }

    private void bindMdc(BookingSaga saga, Booking booking) {
        if (saga.getId() != null) {
            MDC.put("saga_id", saga.getId().toString());
        }
        if (saga.getTraceId() != null && !saga.getTraceId().isBlank()) {
            MDC.put("trace_id", saga.getTraceId());
        }
        if (booking != null) {
            MDC.put("booking_id", booking.getId().toString());
            MDC.put("user_id", booking.getUserId().toString());
            MDC.put("room_id", booking.getRoomId().toString());
        }
    }

    private void clearMdc() {
        MDC.remove("saga_id");
        MDC.remove("booking_id");
        MDC.remove("user_id");
        MDC.remove("room_id");
        MDC.remove("trace_id");
    }

    private String trim(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
