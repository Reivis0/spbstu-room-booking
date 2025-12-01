package com.github.MadyarovGleb.booking_mvp.service;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingConflict {
    private final String bookingId;
    private final String startTime;
    private final String endTime;
    private final String userId;
}