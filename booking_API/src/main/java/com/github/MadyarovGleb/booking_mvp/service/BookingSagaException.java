package com.github.MadyarovGleb.booking_mvp.service;

public class BookingSagaException extends RuntimeException {
    public BookingSagaException(String message, Throwable cause) {
        super(message, cause);
    }
}
