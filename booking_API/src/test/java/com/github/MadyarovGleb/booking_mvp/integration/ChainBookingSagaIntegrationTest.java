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

@SpringBootTest
public class ChainBookingSagaIntegrationTest {

    @Autowired
    private ChainBookingSagaOrchestrator orchestrator;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private AvailabilityEngineClient availabilityEngineClient;

    private UUID testUserId;
    private UUID room1Id;
    private UUID room2Id;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        testUserId = UUID.randomUUID();
        room1Id = UUID.randomUUID();
        room2Id = UUID.randomUUID();
    }

    @Test
    void testSuccessfulChainBooking() {
        // Mock success for both rooms
        ValidationResult successResult = new ValidationResult(true, null, Collections.emptyList());
        
        when(availabilityEngineClient.validate(any(), any(), any(), any()))
                .thenReturn(successResult);

        List<CreateBookingRequest> requests = List.of(
                CreateBookingRequest.builder().roomId(room1Id).startsAt(OffsetDateTime.now().plusHours(1)).endsAt(OffsetDateTime.now().plusHours(2)).build(),
                CreateBookingRequest.builder().roomId(room2Id).startsAt(OffsetDateTime.now().plusHours(2)).endsAt(OffsetDateTime.now().plusHours(3)).build()
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

        List<CreateBookingRequest> requests = List.of(
                CreateBookingRequest.builder().roomId(room1Id).startsAt(OffsetDateTime.now().plusHours(5)).endsAt(OffsetDateTime.now().plusHours(6)).build(),
                CreateBookingRequest.builder().roomId(room2Id).startsAt(OffsetDateTime.now().plusHours(6)).endsAt(OffsetDateTime.now().plusHours(7)).build()
        );

        try {
            orchestrator.execute(testUserId, requests);
        } catch (Exception e) {
            // Expected
        }

        // Verify that ALL bookings in the chain are in REJECTED/CANCELLED state
        List<Booking> allBookings = bookingRepository.findAll();
        assertThat(allBookings).hasSize(2);
        for (Booking b : allBookings) {
            assertThat(b.getStatus()).isEqualTo(BookingStatus.rejected);
            assertThat(b.getCancellationReason()).contains("chain_rollback");
        }
    }
}
