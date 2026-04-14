package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilityEngineClient availability;
    private final NatsPublisher natsPublisher;
    private final RedisService redis;
    private final MeterRegistry meterRegistry;
    private final Counter bookingCreatedCounter;
    private final Counter bookingCancelledCounter;
    private final Counter bookingRejectedCounter;
    private final Counter bookingErrorCounter;
    private final Timer availabilityEngineTimer;

    public BookingService(
            BookingRepository bookingRepository,
            AvailabilityEngineClient availability,
            NatsPublisher natsPublisher,
            RedisService redis,
            MeterRegistry meterRegistry
    ) {
        this.bookingRepository = bookingRepository;
        this.availability = availability;
        this.natsPublisher = natsPublisher;
        this.redis = redis;
        this.meterRegistry = meterRegistry;
        
        this.bookingCreatedCounter = Counter.builder("booking_created_total")
                .description("Total created bookings")
                .register(meterRegistry);
        this.bookingCancelledCounter = Counter.builder("booking_cancelled_total")
                .description("Total cancelled bookings")
                .register(meterRegistry);
        this.bookingRejectedCounter = Counter.builder("booking_rejected_total")
                .description("Total rejected bookings")
                .register(meterRegistry);
        this.bookingErrorCounter = Counter.builder("booking_errors_total")
                .description("Total booking errors")
                .tag("type", "unknown")
                .register(meterRegistry);
        this.availabilityEngineTimer = Timer.builder("availability_engine_response_seconds")
                .description("Time waiting for availability engine response")
                .register(meterRegistry);
    }

    @Transactional
    public Booking createBooking(UUID userId, CreateBookingRequest req) {
        try {
            validateTimes(req);

            ValidationResult result = availabilityEngineTimer.record(() -> 
                availability.validate(req.getRoomId(), req.getStartsAt(), req.getEndsAt(), null)
            );

            List<BookingConflict> realConflicts = result.getConflicts().stream()
                    .filter(c -> c.getBookingId() != null && !c.getBookingId().isEmpty())
                    .toList();

            if (!realConflicts.isEmpty()) {
                bookingRejectedCounter.increment();
                throw new BookingConflictException(realConflicts);
            }

            Booking booking = Booking.builder()
                    .userId(userId)
                    .roomId(req.getRoomId())
                    .startsAt(req.getStartsAt())
                    .endsAt(req.getEndsAt())
                    .status(Booking.BookingStatus.pending)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            Booking saved = bookingRepository.save(booking);

            natsPublisher.publish("booking.created",
                    String.format("{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                            saved.getId(), saved.getUserId(), saved.getRoomId()));

            invalidateCache(saved);
            bookingCreatedCounter.increment();
            return saved;
        } catch (BookingConflictException e) {
            throw e;
        } catch (Exception e) {
            Counter.builder("booking_errors_total")
                .description("Total booking errors")
                .tag("type", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
            throw e;
        }
    }

    @Transactional
    public Booking updateBooking(UUID actorId, String actorRole, UUID bookingId, CreateBookingRequest req) {
        try {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            checkActorPermission(booking, actorId, actorRole);
            if (booking.getStartsAt().isBefore(OffsetDateTime.now()))
                throw new IllegalArgumentException("cannot update a booking that already started");

            ValidationResult result = availabilityEngineTimer.record(() -> 
                availability.validate(req.getRoomId(), req.getStartsAt(), req.getEndsAt(), bookingId)
            );

            List<BookingConflict> realConflicts = result.getConflicts().stream()
                    .filter(c -> c.getBookingId() != null && !c.getBookingId().isEmpty())
                    .toList();

            if (!realConflicts.isEmpty()) {
                bookingRejectedCounter.increment();
                throw new BookingConflictException(realConflicts);
            }

            invalidateCache(booking);

            booking.setRoomId(req.getRoomId());
            booking.setStartsAt(req.getStartsAt());
            booking.setEndsAt(req.getEndsAt());
            booking.setUpdatedAt(OffsetDateTime.now());
            Booking updated = bookingRepository.save(booking);

            natsPublisher.publish("booking.updated",
                    String.format("{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                            updated.getId(), updated.getUserId(), updated.getRoomId()));

            invalidateCache(updated);
            return updated;
        } catch (BookingConflictException e) {
            throw e;
        } catch (Exception e) {
            Counter.builder("booking_errors_total")
                .description("Total booking errors")
                .tag("type", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
            throw e;
        }
    }

    @Transactional
    public Booking cancel(UUID id, UUID actorId, String actorRole) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        checkActorPermission(booking, actorId, actorRole);

        booking.setStatus(Booking.BookingStatus.cancelled);
        Booking cancelled = bookingRepository.save(booking);

        natsPublisher.publish("booking.cancelled",
                String.format("{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                        cancelled.getId(), cancelled.getUserId(), cancelled.getRoomId()));

        invalidateCache(cancelled);
        bookingCancelledCounter.increment();
        return cancelled;
    }

    private void validateTimes(CreateBookingRequest req) {
        if (req.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("starts_at in the past");
        if (!req.getEndsAt().isAfter(req.getStartsAt()))
            throw new IllegalArgumentException("ends_at must be after starts_at");
    }

    private void checkActorPermission(Booking booking, UUID actorId, String actorRole) {
        if (!booking.getUserId().equals(actorId) && !"admin".equals(actorRole))
            throw new SecurityException("forbidden");
    }

    private void invalidateCache(Booking booking) {
        redis.delete(String.format("availability:%s:%s", booking.getRoomId(), booking.getStartsAt().toLocalDate()));
        redis.deletePattern("rooms:search:*");
    }

    public Booking getById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }

    public List<Booking> listByUser(UUID userId) {
        return bookingRepository.findByUserId(userId);
    }
}
