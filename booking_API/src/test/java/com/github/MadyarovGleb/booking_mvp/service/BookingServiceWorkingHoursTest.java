package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.InvalidBookingTimeException;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.outbox.OutboxService;
import com.github.MadyarovGleb.booking_mvp.service.saga.BookingSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для проверки рабочих часов (8:00-21:00)")
class BookingServiceWorkingHoursTest {

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

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
    }

    // --- WORKING HOURS VALIDATION ---

    @Test
    @DisplayName("Бронирование вне рабочих часов: Начало в 07:59 (до 08:00)")
    void createBooking_ShouldThrowInvalidBookingTime_WhenStartsBefore8AM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(7, 59), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(9, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Early Meeting")
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("Booking start time must be at or after 08:00");
    }

    @Test
    @DisplayName("Бронирование вне рабочих часов: Начало в 07:00")
    void createBooking_ShouldThrowInvalidBookingTime_WhenStartsAt7AM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(7, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(8, 30), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Very Early Meeting")
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("08:00 - 21:00");
    }

    @Test
    @DisplayName("Бронирование вне рабочих часов: Конец в 21:01 (после 21:00)")
    void createBooking_ShouldThrowInvalidBookingTime_WhenEndsAfter9PM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(20, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(21, 1), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Late Meeting")
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("Booking end time must be at or before 21:00");
    }

    @Test
    @DisplayName("Бронирование вне рабочих часов: Конец в 22:00")
    void createBooking_ShouldThrowInvalidBookingTime_WhenEndsAt10PM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(20, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(22, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Very Late Meeting")
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(userId, req))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("08:00 - 21:00");
    }

    @Test
    @DisplayName("Бронирование в рабочие часы: Начало в 08:00 (граница)")
    void createBooking_ShouldSucceed_WhenStartsAt8AM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(8, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(9, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Morning Meeting")
                .build();

        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        // Should not throw
        bookingService.createBooking(userId, req);
    }

    @Test
    @DisplayName("Бронирование в рабочие часы: Конец в 21:00 (граница)")
    void createBooking_ShouldSucceed_WhenEndsAt9PM() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(20, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(21, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Evening Meeting")
                .build();

        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        // Should not throw
        bookingService.createBooking(userId, req);
    }

    @Test
    @DisplayName("Бронирование в рабочие часы: Полный рабочий день (08:00-21:00)")
    void createBooking_ShouldSucceed_WhenCoversFullWorkingDay() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(8, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(21, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Full Day Event")
                .build();

        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        // Should not throw
        bookingService.createBooking(userId, req);
    }

    @Test
    @DisplayName("Бронирование в рабочие часы: Середина дня (12:00-14:00)")
    void createBooking_ShouldSucceed_WhenInMiddleOfWorkingHours() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        OffsetDateTime startsAt = OffsetDateTime.of(tomorrow, LocalTime.of(12, 0), ZoneOffset.UTC);
        OffsetDateTime endsAt = OffsetDateTime.of(tomorrow, LocalTime.of(14, 0), ZoneOffset.UTC);

        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(roomId)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .title("Lunch Meeting")
                .build();

        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        // Should not throw
        bookingService.createBooking(userId, req);
    }
}
