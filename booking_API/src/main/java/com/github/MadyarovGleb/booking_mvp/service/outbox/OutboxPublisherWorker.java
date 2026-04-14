package com.github.MadyarovGleb.booking_mvp.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.MadyarovGleb.booking_mvp.entity.OutboxEvent;
import com.github.MadyarovGleb.booking_mvp.repository.OutboxEventRepository;
import com.github.MadyarovGleb.booking_mvp.service.nats.NatsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxPublisherWorker {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisherWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final NatsPublisher natsPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate txTemplate;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    public OutboxPublisherWorker(
            OutboxEventRepository outboxEventRepository,
            NatsPublisher natsPublisher,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.natsPublisher = natsPublisher;
        this.objectMapper = objectMapper;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.delay-ms:1000}")
    public void publishPendingEvents() {
        List<UUID> ids = txTemplate.execute(status -> claimBatchIds());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (UUID id : ids) {
            txTemplate.executeWithoutResult(status -> publishSingle(id));
        }
    }

    private List<UUID> claimBatchIds() {
        return outboxEventRepository.lockUnpublished(batchSize)
                .stream()
                .map(OutboxEvent::getId)
                .toList();
    }

    private void publishSingle(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || Boolean.TRUE.equals(event.getPublished())) {
            return;
        }

        if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
            MDC.put("trace_id", event.getTraceId());
        }
        try {
            event.setPublishAttempts(event.getPublishAttempts() + 1);

            Optional<OutboxEventType> eventTypeOpt = OutboxEventType.fromValue(event.getEventType());
            if (eventTypeOpt.isEmpty()) {
                String err = "Unknown outbox event type: " + event.getEventType();
                event.setLastError(err);
                logger.error(err);
                return;
            }

            String payload = toJson(event);
            natsPublisher.publish(eventTypeOpt.get().subject(), payload);
            event.setPublished(true);
            event.setPublishedAt(OffsetDateTime.now());
            event.setLastError(null);
            logger.info("Outbox event published event_id={} event_type={}", event.getId(), event.getEventType());
        } catch (Exception e) {
            event.setLastError(trim(e.getMessage(), 1000));
            logger.warn("Outbox publish failed event_id={} event_type={} attempt={} reason={}",
                    event.getId(), event.getEventType(), event.getPublishAttempts(), e.getClass().getSimpleName());
        } finally {
            MDC.remove("trace_id");
        }
    }

    private String toJson(OutboxEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event.getPayload());
    }

    private String trim(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
