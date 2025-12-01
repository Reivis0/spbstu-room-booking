package com.github.MadyarovGleb.booking_mvp.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BookingConflictException extends RuntimeException {
    private final List<BookingConflict> conflicts;
}