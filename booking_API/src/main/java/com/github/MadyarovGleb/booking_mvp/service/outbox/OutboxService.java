package com.github.MadyarovGleb.booking_mvp.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.MadyarovGleb.booking_mvp.entity.OutboxEvent;
import com.github.MadyarovGleb.booking_mvp.repository.OutboxEventRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void addEvent(OutboxEventType eventType, Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>(payload);
        String traceId = MDC.get("trace_id");
        if (traceId != null && !traceId.isBlank()) {
            envelope.putIfAbsent("trace_id", traceId);
        }

        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType.name())
                .payload(objectMapper.valueToTree(envelope))
                .traceId(traceId)
                .published(false)
                .publishAttempts(0)
                .build();
        outboxEventRepository.save(event);
    }
}
