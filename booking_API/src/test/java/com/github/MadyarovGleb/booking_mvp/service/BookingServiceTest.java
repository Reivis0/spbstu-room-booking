package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.ConflictException;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.exception.ValidationException;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxEventType;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxService;
import com.github.MadyarovGleb.booking_mvp.service.saga.BookingSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для BookingService")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityEngineClient availability;

    @Mock
    private OutboxService outboxService;

    @Mock
    private BookingSagaService bookingSagaService;

    @Mock
    private RedisService redis;

    @InjectMocks
    private BookingService bookingService;

    private UUID userId;
    private UUID roomId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    // --- CREATE BOOKING ---

    @Test
    @DisplayName("Создание брони: Успех")
    void createBooking_ShouldSucceed() {
        CreateBookingRequest req = createRequest(1, 2);
        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(bookingId);
            return b;
        });

        Booking result = bookingService.createBooking(userId, req);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.pending);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(bookingSagaService).startSaga(any(Booking.class));
        verify(redis).deletePattern(anyString());
    }

    @Test
    @DisplayName("Создание брони: Ошибка валидации (null запрос)")
    void createBooking_ShouldThrowValidation_WhenRequestIsNull() {
        assertThatThrownBy(() -> bookingService.createBooking(userId, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("request body is required");
    }

    @Test
    @DisplayName("Создание брони: Ошибка валидации (отсутствует roomId)")
    void createBooking_ShouldThrowValidation_WhenRoomIdIsNull() {
        CreateBookingRequest req = createRequest(1, 2);
        req.setRoomId(null);
        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("room_id is required");
    }

    @Test
    @DisplayName("Создание брони: Ошибка валидации (отсутствует время)")
    void createBooking_ShouldThrowValidation_WhenTimesAreNull() {
        CreateBookingRequest req = createRequest(1, 2);
        req.setStartsAt(null);
        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("starts_at and ends_at are required");
    }

    @Test
    @DisplayName("Создание брони: Ошибка валидации (время в прошлом)")
    void createBooking_ShouldThrowValidation_WhenStartsAtInPast() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().minusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(1))
                .build();
        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("starts_at in the past");
    }

    @Test
    @DisplayName("Создание брони: Ошибка валидации (конец раньше начала)")
    void createBooking_ShouldThrowValidation_WhenEndsAtNotAfterStartsAt() {
        OffsetDateTime now = OffsetDateTime.now().plusHours(1);
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(now)
                .endsAt(now.minusMinutes(1))
                .build();
        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ends_at must be after starts_at");
    }

    // --- UPDATE BOOKING ---

    @Test
    @DisplayName("Обновление брони: Успех")
    void updateBooking_ShouldSucceed() {
        Booking existing = createBooking(userId, 2, 3);
        CreateBookingRequest req = createRequest(4, 5);
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(true, null, Collections.emptyList()));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateBooking(userId, "user", bookingId, req);

        assertThat(result.getStartsAt()).isEqualTo(req.getStartsAt());
        verify(outboxService).addEvent(eq(OutboxEventType.BOOKING_UPDATED), anyMap());
    }

    @Test
    @DisplayName("Обновление брони: Успех (Администратором)")
    void updateBooking_ShouldSucceed_WhenActorIsAdmin() {
        Booking existing = createBooking(UUID.randomUUID(), 2, 3); // Чужая бронь
        CreateBookingRequest req = createRequest(4, 5);
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(true, null, Collections.emptyList()));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Case: userId mismatch, role admin
        Booking result = bookingService.updateBooking(UUID.randomUUID(), "admin", bookingId, req);

        assertThat(result.getStartsAt()).isEqualTo(req.getStartsAt());
    }

    @Test
    @DisplayName("Обновление брони: Успех (Владелец-Админ)")
    void updateBooking_ShouldSucceed_WhenActorIsOwnerAndAdmin() {
        Booking existing = createBooking(userId, 2, 3); 
        CreateBookingRequest req = createRequest(4, 5);
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(true, null, Collections.emptyList()));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Case: userId match, role admin
        Booking result = bookingService.updateBooking(userId, "admin", bookingId, req);

        assertThat(result.getStartsAt()).isEqualTo(req.getStartsAt());
    }

    @Test
    @DisplayName("Обновление брони: Успех (Владелец-Пользователь)")
    void updateBooking_ShouldSucceed_WhenActorIsOwnerAndUser() {
        Booking existing = createBooking(userId, 2, 3);
        CreateBookingRequest req = createRequest(4, 5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(true, null, Collections.emptyList()));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Case: userId match, role user
        Booking result = bookingService.updateBooking(userId, "user", bookingId, req);

        assertThat(result.getStartsAt()).isEqualTo(req.getStartsAt());
    }

    @Test
    @DisplayName("Обновление брони: Ошибка NotFound")
    void updateBooking_ShouldThrowNotFound_WhenBookingMissing() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.updateBooking(userId, "user", bookingId, createRequest(1, 2)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Обновление брони: Ошибка Forbidden")
    void updateBooking_ShouldThrowForbidden_WhenActorIsStranger() {
        Booking existing = createBooking(UUID.randomUUID(), 2, 3);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> bookingService.updateBooking(userId, "user", bookingId, createRequest(1, 2)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Обновление брони: Ошибка Validation (уже началась)")
    void updateBooking_ShouldThrowValidation_WhenAlreadyStarted() {
        Booking existing = createBooking(userId, -1, 1); // Началась час назад
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> bookingService.updateBooking(userId, "user", bookingId, createRequest(2, 3)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot update a booking that already started");
    }

    @Test
    @DisplayName("Обновление брони: Ошибка Conflict")
    void updateBooking_ShouldThrowConflict_WhenAvailabilityEngineDetectsOverlap() {
        Booking existing = createBooking(userId, 2, 3);
        CreateBookingRequest req = createRequest(4, 5);
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(availability.validate(any(), any(), any(), eq(bookingId)))
                .thenReturn(new ValidationResult(false, null, List.of(new BookingConflict("other", "04:00", "05:00", "u2"))));

        assertThatThrownBy(() -> bookingService.updateBooking(userId, "user", bookingId, req))
                .isInstanceOf(ConflictException.class);
    }

    // --- CANCEL BOOKING ---

    @Test
    @DisplayName("Отмена брони: Успех")
    void cancel_ShouldSucceed() {
        Booking existing = createBooking(userId, 1, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancel(bookingId, userId, "user");

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.cancelled);
        verify(outboxService).addEvent(eq(OutboxEventType.BOOKING_CANCELLED), anyMap());
    }

    @Test
    @DisplayName("Отмена брони: Ошибка NotFound")
    void cancel_ShouldThrowNotFound_WhenMissing() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.cancel(bookingId, userId, "user"))
                .isInstanceOf(NotFoundException.class);
    }

    // --- GET & LIST ---

    @Test
    @DisplayName("Получение по ID: Успех")
    void getById_ShouldSucceed() {
        Booking b = createBooking(userId, 1, 2);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        Booking result = bookingService.getById(bookingId);

        assertThat(result).isEqualTo(b);
    }

    @Test
    @DisplayName("Получение по ID: Ошибка NotFound")
    void getById_ShouldThrowNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.getById(bookingId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Список по пользователю: Успех")
    void listByUser_ShouldSucceed() {
        List<Booking> bookings = List.of(createBooking(userId, 1, 2));
        when(bookingRepository.findByUserId(userId)).thenReturn(bookings);

        List<Booking> result = bookingService.listByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    // --- HELPER METHODS ---

    private CreateBookingRequest createRequest(int startHours, int endHours) {
        return CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().plusHours(startHours))
                .endsAt(OffsetDateTime.now().plusHours(endHours))
                .build();
    }

    private Booking createBooking(UUID uid, int startHours, int endHours) {
        return Booking.builder()
                .id(bookingId)
                .userId(uid)
                .roomId(roomId)
                .startsAt(OffsetDateTime.now().plusHours(startHours))
                .endsAt(OffsetDateTime.now().plusHours(endHours))
                .status(Booking.BookingStatus.pending)
                .build();
    }
}
