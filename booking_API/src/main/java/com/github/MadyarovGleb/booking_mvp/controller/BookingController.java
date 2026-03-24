package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (UUID) auth.getPrincipal() : null;
    }

    private String getCurrentUserRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "").toLowerCase();
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateBookingRequest body) {
        var userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).build();
        var booking = bookingService.createBooking(userId, body);
        return ResponseEntity.status(201).body(booking);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> my() {
        var userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        var booking = bookingService.getById(id);
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (!booking.getUserId().equals(userId) && (role == null || !role.equals("admin"))) {
            throw new ForbiddenException("forbidden");
        }
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CreateBookingRequest body) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) return ResponseEntity.status(401).build();
        var booking = bookingService.updateBooking(userId, role, id, body);
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) return ResponseEntity.status(401).build();
        bookingService.cancel(id, userId, role);
        return ResponseEntity.noContent().build();
    }
}
