package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    AvailabilityEngineClient availability;

    @Mock
    NatsPublisher natsPublisher;

    @Mock
    RedisService redisService;

    @InjectMocks
    BookingService bookingService;

    UUID userId;
    UUID roomId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
    }

    // ===================== CREATE =====================

    @Test
    void createBooking_success() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(start)
                .endsAt(end)
                .build();

        when(availability.validate(any(), any(), any(), isNull()))
                .thenReturn(new ValidationResult(true, null, List.of()));

        when(bookingRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(userId, req);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(roomId, result.getRoomId());
        assertEquals(Booking.BookingStatus.pending, result.getStatus());

        verify(natsPublisher).publish(eq("booking.created"), anyString());
        verify(redisService).deletePattern("rooms:search:*");
    }

    @Test
    void createBooking_conflict_throwsException() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(start)
                .endsAt(end)
                .build();

        BookingConflict conflict = new BookingConflict(
                UUID.randomUUID().toString(),
                "10:00",
                "11:00",
                UUID.randomUUID().toString()
        );

        when(availability.validate(any(), any(), any(), isNull()))
                .thenReturn(new ValidationResult(false, null, List.of(conflict)));

        BookingConflictException ex = assertThrows(
                BookingConflictException.class,
                () -> bookingService.createBooking(userId, req)
        );

        assertEquals(1, ex.getConflicts().size());
        verifyNoInteractions(natsPublisher);
    }

    @Test
    void createBooking_startsInPast_throwsException() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().minusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(1))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, req));
    }

    // ===================== UPDATE =====================

    @Test
    void updateBooking_success() {
        UUID bookingId = UUID.randomUUID();

        Booking existing = Booking.builder()
                .id(bookingId)
                .userId(userId)
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().plusHours(2))
                .endsAt(OffsetDateTime.now().plusHours(3))
                .status(Booking.BookingStatus.pending)
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(existing));

        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(true, null, List.of()));

        when(bookingRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusHours(4))
                .endsAt(OffsetDateTime.now().plusHours(5))
                .build();

        Booking updated = bookingService.updateBooking(
                userId, "user", bookingId, req
        );

        assertEquals(req.getRoomId(), updated.getRoomId());
        verify(natsPublisher).publish(eq("booking.updated"), anyString());
    }

    @Test
    void updateBooking_forbidden() {
        UUID bookingId = UUID.randomUUID();

        Booking existing = Booking.builder()
                .id(bookingId)
                .userId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(2))
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () ->
                bookingService.updateBooking(userId, "user", bookingId,
                        CreateBookingRequest.builder().build()));
    }

    // ===================== CANCEL =====================

    @Test
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .id(bookingId)
                .userId(userId)
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().plusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(2))
                .status(Booking.BookingStatus.pending)
                .build();

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking cancelled = bookingService.cancel(bookingId, userId, "user");

        assertEquals(Booking.BookingStatus.cancelled, cancelled.getStatus());

        verify(natsPublisher).publish(eq("booking.cancelled"), anyString());
        verify(redisService).delete(anyString());
    }
}
