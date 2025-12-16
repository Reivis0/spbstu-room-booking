package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient.BookingConflict;
import lombok.Getter;

import java.util.List;

@Getter
public class BookingConflictException extends RuntimeException {

    private final List<BookingConflict> conflicts;

    public BookingConflictException(List<BookingConflict> conflicts) {
        super(buildMessage(conflicts));
        this.conflicts = conflicts;
    }

    private static String buildMessage(List<BookingConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "Booking conflict occurred";
        }
        StringBuilder sb = new StringBuilder("Booking conflicts: ");
        for (BookingConflict c : conflicts) {
            sb.append(String.format("[bookingId=%s, start=%s, end=%s, userId=%s] ",
                    c.getBookingId(), c.getStartTime(), c.getEndTime(), c.getUserId()));
        }
        return sb.toString();
    }
}
