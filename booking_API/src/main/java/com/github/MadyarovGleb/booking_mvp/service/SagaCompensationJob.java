package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.SagaCompensationOutbox;
import com.github.MadyarovGleb.booking_mvp.repository.SagaCompensationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SagaCompensationJob {

    private final SagaCompensationOutboxRepository outboxRepository;

    @Scheduled(fixedDelay = 30000)
    public void processCompensationOutbox() {
        List<SagaCompensationOutbox> pending = outboxRepository.findByStatus("PENDING");
        for (SagaCompensationOutbox outbox : pending) {
            try {
                // simulate remote call
                log.info("Processing compensation for outbox {} (booking {})", outbox.getId(), outbox.getBookingId());
                
                outbox.setStatus("PROCESSED");
                outbox.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(outbox);
            } catch (Exception e) {
                log.error("Failed to process compensation for outbox {}", outbox.getId(), e);
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                outboxRepository.save(outbox);
            }
        }
    }
}
