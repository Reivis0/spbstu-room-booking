package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.Booking.BookingStatus;
import com.github.MadyarovGleb.booking_mvp.entity.SagaCompensationOutbox;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.repository.SagaCompensationOutboxRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final AvailabilityEngineClient availabilityEngineClient;
    private final MeterRegistry meterRegistry;
    private final NatsPublisher natsPublisher;
    private final RedisService redis;
    private final SagaCompensationOutboxRepository outboxRepository;

    private final Counter bookingCreatedCounter;
    private final Counter bookingRejectedCounter;
    private final Counter bookingErrorCounter;
    private final Timer availabilityEngineTimer;

    public BookingSagaOrchestrator(
            BookingRepository bookingRepository,
            AvailabilityEngineClient availabilityEngineClient,
            MeterRegistry meterRegistry,
            NatsPublisher natsPublisher,
            RedisService redis,
            SagaCompensationOutboxRepository outboxRepository) {
        this.bookingRepository = bookingRepository;
        this.availabilityEngineClient = availabilityEngineClient;
        this.meterRegistry = meterRegistry;
        this.natsPublisher = natsPublisher;
        this.redis = redis;
        this.outboxRepository = outboxRepository;

        this.bookingCreatedCounter = meterRegistry.counter("booking_created_total", "source", "saga");
        this.bookingRejectedCounter = meterRegistry.counter("booking_rejected_total", "source", "saga");
        this.bookingErrorCounter = meterRegistry.counter("booking_errors_total", "type", "saga");
        this.availabilityEngineTimer = meterRegistry.timer("availability_engine_response_seconds", "source", "saga");
    }

    @Transactional
    public Booking execute(UUID userId, CreateBookingRequest command) {
        // Step 1: local transaction
        Booking booking = createPendingBooking(userId, command);

        try {
            // Step 2: remote call
            ValidationResult response = availabilityEngineTimer.record(
                    () -> availabilityEngineClient.validate(booking.getRoomId(), booking.getStartsAt(), booking.getEndsAt(), null)
            );

            List<BookingConflict> realConflicts = response.getConflicts().stream()
                    .filter(c -> c.getBookingId() != null && !c.getBookingId().isEmpty())
                    .toList();

            if (!realConflicts.isEmpty()) {
                compensate(booking, "slot_not_available");
                throw new BookingConflictException(realConflicts);
            }

            // Step 3a: confirm
            booking.setStatus(BookingStatus.confirmed);
            bookingCreatedCounter.increment();
            Booking saved = bookingRepository.save(booking);
            
            natsPublisher.publish("booking.created",
                    String.format("{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                            saved.getId(), saved.getUserId(), saved.getRoomId()));
            invalidateCache(saved);

            return saved;
        } catch (BookingConflictException e) {
            bookingRejectedCounter.increment();
            throw e;
        } catch (Exception e) {
            log.error("SAGA failed for booking {}, starting compensation", booking.getId(), e);
            compensate(booking, e.getClass().getSimpleName());
            bookingErrorCounter.increment();
            throw new BookingSagaException("Booking failed, compensation applied", e);
        }
    }

    private Booking createPendingBooking(UUID userId, CreateBookingRequest req) {
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(req.getRoomId())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .status(BookingStatus.pending)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return bookingRepository.save(booking);
    }

    private void compensate(Booking booking, String reason) {
        booking.setStatus(BookingStatus.compensation_pending);
        bookingRepository.save(booking);

        try {
            // Dummy cancel call as there is no real cancel in availability engine yet
            log.info("Cancelling reservation in availability engine for booking {}", booking.getId());
            // availabilityEngineClient.cancelReservation(booking.getId());
            booking.setStatus(BookingStatus.compensated);
        } catch (Exception e) {
            log.error("Compensation failed for booking {}: {}", booking.getId(), e.getMessage());
            SagaCompensationOutbox outbox = SagaCompensationOutbox.builder()
                    .bookingId(booking.getId())
                    .status("PENDING")
                    .payload("{\"reason\":\"" + reason + "\"}")
                    .build();
            outboxRepository.save(outbox);
        }
        bookingRepository.save(booking);
    }

    private void invalidateCache(Booking booking) {
        redis.delete(String.format("availability:%s:%s", booking.getRoomId(), booking.getStartsAt().toLocalDate()));
        redis.deletePattern("rooms:search:*");
    }
}