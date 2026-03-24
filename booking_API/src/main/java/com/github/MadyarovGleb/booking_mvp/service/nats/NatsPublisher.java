package com.github.MadyarovGleb.booking_mvp.service.nats;

import io.nats.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NatsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NatsPublisher.class);

    private final Connection nc;

    public NatsPublisher(Connection nc) {
        this.nc = nc;
    }

    public void publish(String subject, String payload) {
        try {
            nc.publish(subject, payload.getBytes());
            logger.info("Published event to NATS subject={}", subject);
        } catch (Exception e) {
            logger.error("Failed to publish event to NATS subject={}", subject, e);
            throw new RuntimeException("Failed to publish message to NATS", e);
        }
    }
}
