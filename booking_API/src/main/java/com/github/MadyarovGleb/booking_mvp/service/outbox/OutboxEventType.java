package com.github.MadyarovGleb.booking_mvp.service.outbox;

import java.util.Arrays;
import java.util.Optional;

public enum OutboxEventType {
    BOOKING_SAGA_STARTED("booking.saga.started"),
    BOOKING_CONFIRMED("booking.confirmed"),
    BOOKING_REJECTED("booking.rejected"),
    BOOKING_UPDATED("booking.updated"),
    BOOKING_CANCELLED("booking.cancelled");

    private final String subject;

    OutboxEventType(String subject) {
        this.subject = subject;
    }

    public String subject() {
        return subject;
    }

    public static Optional<OutboxEventType> fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value))
                .findFirst();
    }
}
