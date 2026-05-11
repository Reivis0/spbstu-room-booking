package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.exception.InvalidBookingTimeException;
import com.github.MadyarovGleb.booking_mvp.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class BookingValidator {
    private static final Logger logger = LoggerFactory.getLogger(BookingValidator.class);
    private static final LocalTime WORKING_HOURS_START = LocalTime.of(8, 0);
    private static final LocalTime WORKING_HOURS_END = LocalTime.of(21, 0);
    private static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");

    public void validate(CreateBookingRequest req) {
        if (req == null) {
            throw new ValidationException("request body is required");
        }
        if (req.getRoomId() == null) {
            throw new ValidationException("room_id is required");
        }
        if (req.getStartsAt() == null || req.getEndsAt() == null) {
            throw new ValidationException("starts_at and ends_at are required");
        }
        if (req.getStartsAt().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("starts_at in the past");
        }
        if (!req.getEndsAt().isAfter(req.getStartsAt())) {
            throw new ValidationException("ends_at must be after starts_at");
        }

        LocalTime startTime = req.getStartsAt().atZoneSameInstant(MSK_ZONE).toLocalTime();
        LocalTime endTime = req.getEndsAt().atZoneSameInstant(MSK_ZONE).toLocalTime();

        if (startTime.isBefore(WORKING_HOURS_START)) {
            logger.warn("Booking validation failed: start time {} MSK is before working hours start {}", startTime, WORKING_HOURS_START);
            throw new InvalidBookingTimeException("Время начала должно быть не ранее 08:00 (МСК).");
        }

        if (endTime.isAfter(WORKING_HOURS_END)) {
            logger.warn("Booking validation failed: end time {} MSK is after working hours end {}", endTime, WORKING_HOURS_END);
            throw new InvalidBookingTimeException("Время окончания должно быть не позднее 21:00 (МСК).");
        }
    }
}
