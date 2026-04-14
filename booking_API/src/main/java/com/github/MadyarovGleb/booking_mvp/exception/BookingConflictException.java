package com.github.MadyarovGleb.booking_mvp.exception;

import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import java.util.List;

public class BookingConflictException extends RuntimeException {
    private final List<BookingConflict> conflicts;

    public BookingConflictException(List<BookingConflict> conflicts) {
        super("Booking conflict detected");
        this.conflicts = conflicts;
    }

    public List<BookingConflict> getConflicts() {
        return conflicts;
    }
}
