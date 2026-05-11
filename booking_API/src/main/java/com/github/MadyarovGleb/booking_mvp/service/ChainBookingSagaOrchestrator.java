package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.Booking.BookingStatus;
import com.github.MadyarovGleb.booking_mvp.exception.BookingConflictException;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.ValidationResult;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChainBookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final AvailabilityEngineClient availabilityEngineClient;
    private final NatsPublisher natsPublisher;
    private final RedisService redis;
    private final BookingValidator validator;

    /**
     * Executes an atomic chain of bookings using Saga.
     * Rule: All or Nothing.
     */
    @Transactional
    public List<Booking> execute(UUID userId, List<CreateBookingRequest> requests) {
        UUID chainId = UUID.randomUUID();
        log.info("Starting Chain Saga for user {} with chain_id {}", userId, chainId);

        if (requests == null || requests.isEmpty()) {
            throw new com.github.MadyarovGleb.booking_mvp.exception.ValidationException("Chain must contain at least one item");
        }

        for (CreateBookingRequest req : requests) {
            validator.validate(req);
        }

        List<Booking> pendingBookings = new ArrayList<>();

        // Step 1: Create all bookings in PENDING state
        for (CreateBookingRequest req : requests) {
            Booking booking = Booking.builder()
                    .chainId(chainId)
                    .userId(userId)
                    .roomId(req.getRoomId())
                    .startsAt(req.getStartsAt())
                    .endsAt(req.getEndsAt())
                    .title(req.getTitle())
                    .status(BookingStatus.pending)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            pendingBookings.add(bookingRepository.save(booking));
        }

        List<Booking> confirmedBookings = new ArrayList<>();

        try {
            // Step 2: Validate each booking in the chain
            for (Booking booking : pendingBookings) {
                ValidationResult result = availabilityEngineClient.validate(
                        booking.getRoomId(), 
                        booking.getStartsAt(), 
                        booking.getEndsAt(), 
                        null
                );

                if (!result.getConflicts().isEmpty()) {
                    log.warn("Conflict detected for room {} in chain {}. Rollback initiated.", booking.getRoomId(), chainId);
                    throw new BookingConflictException(result.getConflicts());
                }

                // Temporary transition to confirmed (local state)
                booking.setStatus(BookingStatus.confirmed);
                confirmedBookings.add(booking);
            }

            // Step 3: Finalize all
            for (Booking booking : confirmedBookings) {
                bookingRepository.save(booking);
                publishEvent(booking);
                invalidateCache(booking);
            }

            log.info("Chain Saga {} completed successfully", chainId);
            return confirmedBookings;

        } catch (BookingConflictException e) {
            log.warn("Chain Saga {} failed due to conflict, compensating already confirmed records.", chainId);
            compensateChain(chainId, "conflict");
            throw e;
        } catch (Exception e) {
            log.error("Chain Saga {} failed, compensating all records. Reason: {}", chainId, e.getMessage());
            compensateChain(chainId, e.getMessage());
            throw new BookingSagaException("Chain booking failed. Atomic rollback applied for chain " + chainId, e);
        }
    }

    private void compensateChain(UUID chainId, String reason) {
        List<Booking> bookingsToCompensate = bookingRepository.findAll()
                .stream()
                .filter(b -> chainId.equals(b.getChainId()))
                .collect(Collectors.toList());

        for (Booking b : bookingsToCompensate) {
            b.setStatus(BookingStatus.rejected);
            b.setCancellationReason("chain_rollback: " + reason);
            bookingRepository.save(b);
            natsPublisher.publish("booking.rejected", 
                String.format("{\"booking_id\":\"%s\", \"chain_id\":\"%s\"}", b.getId(), chainId));
        }
        redis.deletePattern("rooms:search:*");
    }

    private void publishEvent(Booking saved) {
        natsPublisher.publish("booking.created",
                String.format("{\"booking_id\":\"%s\",\"user_id\":\"%s\",\"room_id\":\"%s\",\"chain_id\":\"%s\"}",
                        saved.getId(), saved.getUserId(), saved.getRoomId(), saved.getChainId()));
    }

    private void invalidateCache(Booking booking) {
        redis.delete(String.format("availability:%s:%s", booking.getRoomId(), booking.getStartsAt().toLocalDate()));
    }
}
