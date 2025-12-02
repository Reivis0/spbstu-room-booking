package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
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

    public BookingService(
            BookingRepository bookingRepository,
            AvailabilityEngineClient availability,
            NatsPublisher natsPublisher,
            RedisService redis
    ) {
        this.bookingRepository = bookingRepository;
        this.availability = availability;
        this.natsPublisher = natsPublisher;
        this.redis = redis;
    }

    @Transactional
    public Booking createBooking(UUID userId, CreateBookingRequest req) {
        validateTimes(req);

        var conflicts = availability.validate(req.getRoomId(), req.getStartsAt(), req.getEndsAt(), null);
        //if (!conflicts.isEmpty()) throw new BookingConflictException(conflicts);

        Booking b = Booking.builder()
                .userId(userId)
                .roomId(req.getRoomId())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .status(Booking.BookingStatus.pending)
                .build();

        Booking saved = bookingRepository.save(b);

        natsPublisher.publish("booking.created", "{\"booking_id\":\"" + saved.getId() +
                "\",\"user_id\":\"" + saved.getUserId() + "\",\"room_id\":\"" + saved.getRoomId() + "\"}");

        // инвалидация кэша
        invalidateCache(saved);

        return saved;
    }

    @Transactional
    public Booking updateBooking(UUID actorId, String actorRole, UUID bookingId, CreateBookingRequest req) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow();
        checkActorPermission(b, actorId, actorRole);
        if (b.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("cannot update a booking that already started");

        var conflicts = availability.validate(req.getRoomId(), req.getStartsAt(), req.getEndsAt(), bookingId);
        //if (!conflicts.isEmpty()) throw new BookingConflictException(conflicts);

        invalidateCache(b);

        b.setRoomId(req.getRoomId());
        b.setStartsAt(req.getStartsAt());
        b.setEndsAt(req.getEndsAt());
        Booking updated = bookingRepository.save(b);

        natsPublisher.publish("booking.updated", "{\"booking_id\":\"" + updated.getId() +
                "\",\"user_id\":\"" + updated.getUserId() + "\",\"room_id\":\"" + updated.getRoomId() + "\"}");

        invalidateCache(updated);
        return updated;
    }

    @Transactional
    public Booking cancel(UUID id, UUID actorId, String actorRole) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        checkActorPermission(b, actorId, actorRole);

        b.setStatus(Booking.BookingStatus.cancelled);
        Booking cancelled = bookingRepository.save(b);

        natsPublisher.publish("booking.cancelled", "{\"booking_id\":\"" + cancelled.getId() +
                "\",\"user_id\":\"" + cancelled.getUserId() + "\",\"room_id\":\"" + cancelled.getRoomId() + "\"}");

        invalidateCache(cancelled);
        return cancelled;
    }

    private void validateTimes(CreateBookingRequest req) {
        if (req.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("starts_at in the past");
        if (!req.getEndsAt().isAfter(req.getStartsAt()))
            throw new IllegalArgumentException("ends_at must be after starts_at");
    }

    private void checkActorPermission(Booking b, UUID actorId, String actorRole) {
        if (!b.getUserId().equals(actorId) && !"admin".equals(actorRole))
            throw new SecurityException("forbidden");
    }

    private void invalidateCache(Booking b) {
        redis.delete(String.format("availability:%s:%s", b.getRoomId(), b.getStartsAt().toLocalDate()));
        redis.deletePattern("rooms:search:*");
    }

    // ───────────────────────────────────────────────
// Получение брони по ID
    public Booking getById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }

    // ───────────────────────────────────────────────
// Получение всех броней пользователя
    public List<Booking> listByUser(UUID userId) {
        return bookingRepository.findByUserId(userId);
    }

}
