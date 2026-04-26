package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.exception.ConflictException;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.exception.ValidationException;
import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxEventType;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxService;
import com.github.MadyarovGleb.booking_mvp.service.saga.BookingSagaService;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final AvailabilityEngineClient availability;
    private final OutboxService outboxService;
    private final BookingSagaService bookingSagaService;
    private final RedisService redis;

    public BookingService(
            BookingRepository bookingRepository,
            AvailabilityEngineClient availability,
            OutboxService outboxService,
            BookingSagaService bookingSagaService,
            RedisService redis
    ) {
        this.bookingRepository = bookingRepository;
        this.availability = availability;
        this.outboxService = outboxService;
        this.bookingSagaService = bookingSagaService;
        this.redis = redis;
    }

    @Retryable(
            value = { CannotAcquireLockException.class, DataIntegrityViolationException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking createBooking(UUID userId, CreateBookingRequest req) {
        MDC.put("user_id", userId.toString());
        validateTimes(req);
        MDC.put("room_id", req.getRoomId().toString());
        logger.info("Booking creation accepted, starting saga");

        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(req.getRoomId())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .title(req.getTitle())
                .status(Booking.BookingStatus.pending)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Booking saved = bookingRepository.saveAndFlush(booking);

        if (saved.getId() != null) {
            MDC.put("booking_id", saved.getId().toString());
        }
        bookingSagaService.startSaga(saved);

        invalidateCache(saved);
        logger.info("Booking created in pending status, saga started");
        return saved;
    }

    @Transactional
    public Booking updateBooking(UUID actorId, String actorRole, UUID bookingId, CreateBookingRequest req) {
        MDC.put("user_id", actorId.toString());
        MDC.put("booking_id", bookingId.toString());
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        checkActorPermission(booking, actorId, actorRole);
        if (booking.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new ValidationException("cannot update a booking that already started");
        validateTimes(req);
        MDC.put("room_id", req.getRoomId().toString());
        logger.info("Booking update validation started");

        ValidationResult result = availability.validate(
                req.getRoomId(), req.getStartsAt(), req.getEndsAt(), bookingId
        );

        List<BookingConflict> realConflicts = result.getConflicts().stream()
                .filter(c -> c.getBookingId() != null && !c.getBookingId().isEmpty())
                .toList();

        if (!realConflicts.isEmpty()) {
            logger.warn("Booking update failed due to conflict conflicts_count={}", realConflicts.size());
            throw new ConflictException("booking_conflict", realConflicts);
        }

        invalidateCache(booking);

        booking.setRoomId(req.getRoomId());
        booking.setStartsAt(req.getStartsAt());
        booking.setEndsAt(req.getEndsAt());
        booking.setTitle(req.getTitle());
        booking.setUpdatedAt(OffsetDateTime.now());
        Booking updated = bookingRepository.save(booking);
        outboxService.addEvent(OutboxEventType.BOOKING_UPDATED, eventPayload(updated));

        invalidateCache(updated);
        logger.info("Booking updated successfully");
        return updated;
    }

    @Transactional
    public Booking cancel(UUID id, UUID actorId, String actorRole) {
        MDC.put("user_id", actorId.toString());
        MDC.put("booking_id", id.toString());
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        checkActorPermission(booking, actorId, actorRole);

        booking.setStatus(Booking.BookingStatus.cancelled);
        Booking cancelled = bookingRepository.save(booking);
        outboxService.addEvent(OutboxEventType.BOOKING_CANCELLED, eventPayload(cancelled));

        invalidateCache(cancelled);
        logger.info("Booking cancelled successfully");
        return cancelled;
    }

    private void validateTimes(CreateBookingRequest req) {
        if (req == null) {
            logger.warn("Booking validation failed because request body is missing");
            throw new ValidationException("request body is required");
        }
        if (req.getRoomId() == null) {
            logger.warn("Booking validation failed because room_id is missing");
            throw new ValidationException("room_id is required");
        }
        if (req.getStartsAt() == null || req.getEndsAt() == null) {
            logger.warn("Booking validation failed because time bounds are missing");
            throw new ValidationException("starts_at and ends_at are required");
        }
        if (req.getStartsAt().isBefore(OffsetDateTime.now())) {
            logger.warn("Booking validation failed because starts_at is in the past");
            throw new ValidationException("starts_at in the past");
        }
        if (!req.getEndsAt().isAfter(req.getStartsAt())) {
            logger.warn("Booking validation failed because ends_at is not after starts_at");
            throw new ValidationException("ends_at must be after starts_at");
        }
    }

    private void checkActorPermission(Booking booking, UUID actorId, String actorRole) {
        if (!booking.getUserId().equals(actorId) && !"admin".equals(actorRole)) {
            logger.warn("Booking operation rejected due to insufficient permissions");
            throw new ForbiddenException("forbidden");
        }
    }

    private void invalidateCache(Booking booking) {
        redis.delete(String.format("availability:%s:%s", booking.getRoomId(), booking.getStartsAt().toLocalDate()));
        redis.deletePattern("rooms:search:*");
        logger.debug("Booking related cache invalidated");
    }

    public Booking getById(UUID id) {
        MDC.put("booking_id", id.toString());
        logger.debug("Fetching booking by id");
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    public List<Booking> listByUser(UUID userId) {
        MDC.put("user_id", userId.toString());
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        logger.info("Fetched bookings by user count={}", bookings.size());
        return bookings;
    }

    public List<Booking> listAll() {
        logger.info("Fetching all bookings");
        List<Booking> bookings = bookingRepository.findAll();
        logger.info("Fetched all bookings count={}", bookings.size());
        return bookings;
    }

    private Map<String, Object> eventPayload(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("booking_id", booking.getId().toString());
        payload.put("user_id", booking.getUserId().toString());
        payload.put("room_id", booking.getRoomId().toString());
        payload.put("starts_at", booking.getStartsAt().toString());
        payload.put("ends_at", booking.getEndsAt().toString());
        payload.put("status", booking.getStatus().name().toLowerCase());
        return payload;
    }
}
