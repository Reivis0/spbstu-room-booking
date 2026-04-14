package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.ConflictException;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.exception.ValidationException;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxEventType;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxService;
import com.github.MadyarovGleb.booking_mvp.service.saga.BookingSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
    OutboxService outboxService;

    @Mock
    BookingSagaService bookingSagaService;

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

        when(bookingRepository.saveAndFlush(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(userId, req);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(roomId, result.getRoomId());
        assertEquals(Booking.BookingStatus.pending, result.getStatus());

        verify(bookingSagaService).startSaga(any(Booking.class));
        verify(redisService).deletePattern("rooms:search:*");
        verifyNoInteractions(availability);
    }

    @Test
    void createBooking_doesNotRunExternalValidationSynchronously() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(start)
                .endsAt(end)
                .build();

        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = bookingService.createBooking(userId, req);

        assertNotNull(booking);
        verifyNoInteractions(availability);
        verify(bookingSagaService).startSaga(any(Booking.class));
    }

    @Test
    void createBooking_startsInPast_throwsException() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().minusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(1))
                .build();

        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(userId, req));
    }

    @Test
    void createBooking_dbOverlap_throwsConflict() {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(start)
                .endsAt(end)
                .build();

        when(bookingRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("no_overlap"));

        assertThrows(ConflictException.class,
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
        verify(outboxService).addEvent(eq(OutboxEventType.BOOKING_UPDATED), anyMap());
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

        assertThrows(ForbiddenException.class, () ->
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

        verify(outboxService).addEvent(eq(OutboxEventType.BOOKING_CANCELLED), anyMap());
        verify(redisService).delete(anyString());
    }
}
