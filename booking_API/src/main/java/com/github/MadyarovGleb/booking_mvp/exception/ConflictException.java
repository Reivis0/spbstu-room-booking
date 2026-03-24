package com.github.MadyarovGleb.booking_mvp.exception;

public class ConflictException extends RuntimeException {

    private final Object details;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, Object details) {
        super(message);
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}
