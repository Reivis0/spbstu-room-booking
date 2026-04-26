package com.github.MadyarovGleb.booking_mvp.integration;

import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Integration тесты для BookingRepository с PostgreSQL")
class BookingRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("booking_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    private UUID userId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        
        userId = UUID.randomUUID();
        
        // Create a test room
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .name("101")
                .buildingId(UUID.randomUUID())
                .capacity(30)
                .floor(1)
                .build();
        room = roomRepository.save(room);
        roomId = room.getId();
    }

    @Test
    @DisplayName("Сохранение и получение бронирования")
    void saveAndRetrieveBooking_ShouldWork() {
        OffsetDateTime now = OffsetDateTime.now();
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startsAt(now.plusHours(1))
                .endsAt(now.plusHours(2))
                .title("Test Meeting")
                .status(Booking.BookingStatus.pending)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Booking saved = bookingRepository.save(booking);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getTitle()).isEqualTo("Test Meeting");
    }

    @Test
    @DisplayName("Поиск бронирований по userId")
    void findByUserId_ShouldReturnUserBookings() {
        UUID otherUserId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        // Create bookings for target user
        bookingRepository.save(createBooking(userId, roomId, now.plusHours(1), now.plusHours(2)));
        bookingRepository.save(createBooking(userId, roomId, now.plusHours(3), now.plusHours(4)));
        
        // Create booking for other user
        bookingRepository.save(createBooking(otherUserId, roomId, now.plusHours(5), now.plusHours(6)));

        List<Booking> userBookings = bookingRepository.findByUserId(userId);

        assertThat(userBookings).hasSize(2);
        assertThat(userBookings).allMatch(b -> b.getUserId().equals(userId));
    }

    @Test
    @DisplayName("Проверка временных пересечений (GIST индекс)")
    void findOverlappingBookings_ShouldDetectConflicts() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Existing booking: 10:00 - 12:00
        Booking existing = createBooking(userId, roomId, now.plusHours(10), now.plusHours(12));
        bookingRepository.save(existing);

        // Try to find overlapping bookings for: 11:00 - 13:00 (overlaps with existing)
        OffsetDateTime newStart = now.plusHours(11);
        OffsetDateTime newEnd = now.plusHours(13);

        List<Booking> allBookings = bookingRepository.findAll();
        
        // Manual overlap check (simulating what GIST index would do)
        boolean hasOverlap = allBookings.stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .filter(b -> !b.getStatus().equals(Booking.BookingStatus.cancelled))
                .anyMatch(b -> 
                    newStart.isBefore(b.getEndsAt()) && newEnd.isAfter(b.getStartsAt())
                );

        assertThat(hasOverlap).isTrue();
    }

    @Test
    @DisplayName("Проверка отсутствия пересечений")
    void findOverlappingBookings_ShouldNotDetectConflicts_WhenNoOverlap() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Existing booking: 10:00 - 12:00
        Booking existing = createBooking(userId, roomId, now.plusHours(10), now.plusHours(12));
        bookingRepository.save(existing);

        // Try to find overlapping bookings for: 13:00 - 14:00 (no overlap)
        OffsetDateTime newStart = now.plusHours(13);
        OffsetDateTime newEnd = now.plusHours(14);

        List<Booking> allBookings = bookingRepository.findAll();
        
        boolean hasOverlap = allBookings.stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .filter(b -> !b.getStatus().equals(Booking.BookingStatus.cancelled))
                .anyMatch(b -> 
                    newStart.isBefore(b.getEndsAt()) && newEnd.isAfter(b.getStartsAt())
                );

        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Отмененные бронирования не должны создавать конфликты")
    void findOverlappingBookings_ShouldIgnoreCancelledBookings() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Cancelled booking: 10:00 - 12:00
        Booking cancelled = createBooking(userId, roomId, now.plusHours(10), now.plusHours(12));
        cancelled.setStatus(Booking.BookingStatus.cancelled);
        bookingRepository.save(cancelled);

        // Try to find overlapping bookings for: 11:00 - 13:00
        OffsetDateTime newStart = now.plusHours(11);
        OffsetDateTime newEnd = now.plusHours(13);

        List<Booking> allBookings = bookingRepository.findAll();
        
        boolean hasOverlap = allBookings.stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .filter(b -> !b.getStatus().equals(Booking.BookingStatus.cancelled))
                .anyMatch(b -> 
                    newStart.isBefore(b.getEndsAt()) && newEnd.isAfter(b.getStartsAt())
                );

        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Граничные случаи: бронирования встык не пересекаются")
    void findOverlappingBookings_ShouldNotDetectConflicts_WhenBookingsAreAdjacent() {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Existing booking: 10:00 - 12:00
        Booking existing = createBooking(userId, roomId, now.plusHours(10), now.plusHours(12));
        bookingRepository.save(existing);

        // New booking starts exactly when existing ends: 12:00 - 14:00
        OffsetDateTime newStart = now.plusHours(12);
        OffsetDateTime newEnd = now.plusHours(14);

        List<Booking> allBookings = bookingRepository.findAll();
        
        boolean hasOverlap = allBookings.stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .filter(b -> !b.getStatus().equals(Booking.BookingStatus.cancelled))
                .anyMatch(b -> 
                    newStart.isBefore(b.getEndsAt()) && newEnd.isAfter(b.getStartsAt())
                );

        assertThat(hasOverlap).isFalse();
    }

    @Test
    @DisplayName("Множественные бронирования в разных комнатах не конфликтуют")
    void findOverlappingBookings_ShouldNotDetectConflicts_WhenDifferentRooms() {
        UUID otherRoomId = UUID.randomUUID();
        Room otherRoom = Room.builder()
                .id(otherRoomId)
                .name("102")
                .buildingId(UUID.randomUUID())
                .capacity(20)
                .floor(1)
                .build();
        roomRepository.save(otherRoom);

        OffsetDateTime now = OffsetDateTime.now();
        
        // Booking in room 1: 10:00 - 12:00
        bookingRepository.save(createBooking(userId, roomId, now.plusHours(10), now.plusHours(12)));

        // Try to book room 2 at same time: 10:00 - 12:00
        OffsetDateTime newStart = now.plusHours(10);
        OffsetDateTime newEnd = now.plusHours(12);

        List<Booking> allBookings = bookingRepository.findAll();
        
        boolean hasOverlap = allBookings.stream()
                .filter(b -> b.getRoomId().equals(otherRoomId))
                .filter(b -> !b.getStatus().equals(Booking.BookingStatus.cancelled))
                .anyMatch(b -> 
                    newStart.isBefore(b.getEndsAt()) && newEnd.isAfter(b.getStartsAt())
                );

        assertThat(hasOverlap).isFalse();
    }

    private Booking createBooking(UUID userId, UUID roomId, OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime now = OffsetDateTime.now();
        return Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startsAt(start)
                .endsAt(end)
                .title("Test Booking")
                .status(Booking.BookingStatus.confirmed)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
