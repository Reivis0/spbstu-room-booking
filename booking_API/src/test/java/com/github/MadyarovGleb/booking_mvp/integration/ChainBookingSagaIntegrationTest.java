package com.github.MadyarovGleb.booking_mvp.integration;

import com.github.MadyarovGleb.booking_mvp.dto.ChainBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.Booking.BookingStatus;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.ChainBookingSagaOrchestrator;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.repository.BuildingRepository;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.entity.Building;

import com.github.MadyarovGleb.booking_mvp.repository.UserRepository;
import com.github.MadyarovGleb.booking_mvp.entity.User;

@SpringBootTest
public class ChainBookingSagaIntegrationTest {

    @Autowired
    private ChainBookingSagaOrchestrator orchestrator;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AvailabilityEngineClient availabilityEngineClient;

    private UUID testUserId;
    private UUID room1Id;
    private UUID room2Id;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        buildingRepository.deleteAll();
        userRepository.deleteAll();
        
        User user = User.builder()
                .email("test-" + UUID.randomUUID().toString() + "@test.com")
                .passwordHash("hash")
                .firstname("Test")
                .lastname("Test")
                .build();
        user = userRepository.save(user);
        testUserId = user.getId();

        Building building = Building.builder()
                .name("Saga Building")
                .code("SAGA-" + UUID.randomUUID().toString().substring(0, 8))
                .address("Saga Address")
                .build();
        building = buildingRepository.save(building);

        Room room1 = Room.builder().name("101").code("S101-" + UUID.randomUUID().toString().substring(0, 8)).buildingId(building.getId()).capacity(30).floor(1).build();
        Room room2 = Room.builder().name("102").code("S102-" + UUID.randomUUID().toString().substring(0, 8)).buildingId(building.getId()).capacity(30).floor(1).build();
        
        room1 = roomRepository.save(room1);
        room2 = roomRepository.save(room2);
        
        room1Id = room1.getId();
        room2Id = room2.getId();
    }

    @Test
    void testSuccessfulChainBooking() {
        // Mock success for both rooms
        ValidationResult successResult = new ValidationResult(true, null, Collections.emptyList());
        
        when(availabilityEngineClient.validate(any(), any(), any(), any()))
                .thenReturn(successResult);

        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        List<CreateBookingRequest> requests = List.of(
                CreateBookingRequest.builder().roomId(room1Id).startsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(10, 0), java.time.ZoneOffset.UTC)).endsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(11, 0), java.time.ZoneOffset.UTC)).build(),
                CreateBookingRequest.builder().roomId(room2Id).startsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(11, 0), java.time.ZoneOffset.UTC)).endsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(12, 0), java.time.ZoneOffset.UTC)).build()
        );

        List<Booking> result = orchestrator.execute(testUserId, requests);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.confirmed);
        assertThat(result.get(1).getStatus()).isEqualTo(BookingStatus.confirmed);
        assertThat(result.get(0).getChainId()).isNotNull();
        assertThat(result.get(0).getChainId()).isEqualTo(result.get(1).getChainId());
    }

    @Test
    void testAtomicRollbackWhenOneBookingFails() {
        // Mock success for first room, failure for second
        ValidationResult successResult = new ValidationResult(true, null, Collections.emptyList());

        ValidationResult failureResult = new ValidationResult(false, null, 
                List.of(new AvailabilityEngineClient.BookingConflict("conflict-1", "08:00", "09:00", "other-user")));

        when(availabilityEngineClient.validate(Mockito.eq(room1Id), any(), any(), any()))
                .thenReturn(successResult);
        when(availabilityEngineClient.validate(Mockito.eq(room2Id), any(), any(), any()))
                .thenReturn(failureResult);

        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        List<CreateBookingRequest> requests = List.of(
                CreateBookingRequest.builder().roomId(room1Id).startsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(12, 0), java.time.ZoneOffset.UTC)).endsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(13, 0), java.time.ZoneOffset.UTC)).build(),
                CreateBookingRequest.builder().roomId(room2Id).startsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(13, 0), java.time.ZoneOffset.UTC)).endsAt(OffsetDateTime.of(tomorrow, java.time.LocalTime.of(14, 0), java.time.ZoneOffset.UTC)).build()
        );

        try {
            orchestrator.execute(testUserId, requests);
        } catch (Exception e) {
            // Expected
        }

        // Verify that NO bookings were saved (atomic rollback)
        List<Booking> savedBookings = bookingRepository.findAll();
        assertThat(savedBookings).isEmpty();
    }
}
