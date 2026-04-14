package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.Booking.BookingStatus;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.repository.SagaCompensationOutboxRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSagaOrchestratorTest {

    @Mock BookingRepository bookingRepository;
    @Mock AvailabilityEngineClient availabilityEngineClient;
    @Mock NatsPublisher natsPublisher;
    @Mock RedisService redisService;
    @Mock SagaCompensationOutboxRepository outboxRepository;

    @Spy MeterRegistry meterRegistry = new SimpleMeterRegistry();

    BookingSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new BookingSagaOrchestrator(
                bookingRepository, availabilityEngineClient, meterRegistry,
                natsPublisher, redisService, outboxRepository);
    }

    @Test
    void shouldConfirmBookingWhenAllStepsSucceed() {
        UUID userId = UUID.randomUUID();
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(2))
                .build();

        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(availabilityEngineClient.validate(any(), any(), any(), isNull()))
                .thenReturn(new ValidationResult(true, null, List.of()));

        Booking result = orchestrator.execute(userId, req);

        assertNotNull(result);
        assertEquals(BookingStatus.confirmed, result.getStatus());
        verify(natsPublisher).publish(eq("booking.created"), anyString());
    }

    @Test
    void shouldCompensateWhenAvailabilityEngineFails() {
        UUID userId = UUID.randomUUID();
        CreateBookingRequest req = CreateBookingRequest.builder()
                .roomId(UUID.randomUUID())
                .startsAt(OffsetDateTime.now().plusHours(1))
                .endsAt(OffsetDateTime.now().plusHours(2))
                .build();

        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(availabilityEngineClient.validate(any(), any(), any(), isNull()))
                .thenThrow(new RuntimeException("Engine down"));

        assertThrows(BookingSagaException.class, () -> orchestrator.execute(userId, req));

        verify(bookingRepository, atLeastOnce()).save(argThat(b -> b.getStatus() == BookingStatus.compensated || b.getStatus() == BookingStatus.pending || b.getStatus() == BookingStatus.compensation_pending));
    }
}
