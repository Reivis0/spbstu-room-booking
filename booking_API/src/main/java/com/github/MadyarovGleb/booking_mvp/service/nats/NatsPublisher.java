package com.github.MadyarovGleb.booking_mvp.service.nats;

import io.nats.client.Connection;
import org.springframework.stereotype.Component;

@Component
public class NatsPublisher {

    private final Connection nc;

    public NatsPublisher(Connection nc) {
        this.nc = nc;
    }

    public void publish(String subject, String payload) {
        try {
            nc.publish(subject, payload.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message to NATS", e);
        }
    }
}
