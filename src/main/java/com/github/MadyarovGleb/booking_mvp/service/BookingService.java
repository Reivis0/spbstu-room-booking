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

    public BookingService(
            BookingRepository bookingRepository,
            AvailabilityEngineClient availability,
            NatsPublisher natsPublisher
    ) {
        this.bookingRepository = bookingRepository;
        this.availability = availability;
        this.natsPublisher = natsPublisher;
    }

    // ───────────────────────────────────────────────
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД — заглушка Redis
    private void invalidateCache(String key) {
        // Redis НЕ подключён — ничего не делаем
    }
    // ───────────────────────────────────────────────

    @Transactional
    public Booking createBooking(UUID userId, CreateBookingRequest req) {
        if (req.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("starts_at in the past");
        if (!req.getEndsAt().isAfter(req.getStartsAt()))
            throw new IllegalArgumentException("ends_at must be after starts_at");

        List<BookingConflict> conflicts = availability.validate(
                req.getRoomId(), req.getStartsAt(), req.getEndsAt(), null
        ).stream().map(Conflict -> new BookingConflict(
                Conflict.getBookingId(),
                Conflict.getStartTime(),
                Conflict.getEndTime(),
                Conflict.getUserId()
        )).toList();

        if (!conflicts.isEmpty())
            throw new BookingConflictException(conflicts);

        Booking b = Booking.builder()
                .userId(userId)
                .roomId(req.getRoomId())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .status(Booking.BookingStatus.pending)
                .build();

        Booking saved = bookingRepository.save(b);

        natsPublisher.publish("booking.created", String.format(
                "{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                saved.getId(), saved.getUserId(), saved.getRoomId()
        ));

        invalidateCache(String.format("availability:%s:%s",
                b.getRoomId(), b.getStartsAt().toLocalDate()));

        return saved;
    }

    public Booking getById(UUID id) {
        return bookingRepository.findById(id).orElseThrow();
    }

    public List<Booking> listByUser(UUID userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional
    public Booking updateBooking(UUID actorId, String actorRole, UUID bookingId, CreateBookingRequest req) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow();

        if (!b.getUserId().equals(actorId) && !"admin".equals(actorRole))
            throw new SecurityException("forbidden");

        if (b.getStartsAt().isBefore(OffsetDateTime.now()))
            throw new IllegalArgumentException("cannot update a booking that already started");

        List<BookingConflict> conflicts = availability.validate(
                req.getRoomId(), req.getStartsAt(), req.getEndsAt(), bookingId
        ).stream().map(Conflict -> new BookingConflict(
                Conflict.getBookingId(),
                Conflict.getStartTime(),
                Conflict.getEndTime(),
                Conflict.getUserId()
        )).toList();

        if (!conflicts.isEmpty())
            throw new BookingConflictException(conflicts);

        invalidateCache(String.format("availability:%s:%s",
                b.getRoomId(), b.getStartsAt().toLocalDate()));

        b.setRoomId(req.getRoomId());
        b.setStartsAt(req.getStartsAt());
        b.setEndsAt(req.getEndsAt());

        Booking updated = bookingRepository.save(b);

        natsPublisher.publish("booking.updated", String.format(
                "{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                updated.getId(), updated.getUserId(), updated.getRoomId()
        ));

        invalidateCache(String.format("availability:%s:%s",
                updated.getRoomId(), updated.getStartsAt().toLocalDate()));

        return updated;
    }

    @Transactional
    public Booking cancel(UUID id, UUID actorId, String actorRole) {
        Booking b = bookingRepository.findById(id).orElseThrow();

        if (!b.getUserId().equals(actorId) && !"admin".equals(actorRole))
            throw new SecurityException("forbidden");

        b.setStatus(Booking.BookingStatus.cancelled);
        Booking cancelled = bookingRepository.save(b);

        natsPublisher.publish("booking.cancelled", String.format(
                "{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\"}",
                cancelled.getId(), cancelled.getUserId(), cancelled.getRoomId()
        ));

        invalidateCache(String.format("availability:%s:%s",
                b.getRoomId(), b.getStartsAt().toLocalDate()));

        return cancelled;
    }
}
