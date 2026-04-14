package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.dto.CreateBookingRequest;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.exception.ForbiddenException;
import com.github.MadyarovGleb.booking_mvp.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

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
        if (userId == null) {
            logger.warn("Booking creation rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        if (body != null && body.getRoomId() != null) {
            MDC.put("room_id", body.getRoomId().toString());
        }
        logger.info("Booking creation started");
        var booking = bookingService.createBooking(userId, body);
        MDC.put("booking_id", booking.getId().toString());
        logger.info("Booking creation completed successfully");
        return ResponseEntity.status(201).body(booking);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> my() {
        var userId = getCurrentUserId();
        if (userId == null) {
            logger.warn("My bookings request rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        List<Booking> bookings = bookingService.listByUser(userId);
        logger.info("My bookings retrieved successfully count={}", bookings.size());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        MDC.put("booking_id", id.toString());
        var booking = bookingService.getById(id);
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId != null) {
            MDC.put("user_id", userId.toString());
        }
        if (!booking.getUserId().equals(userId) && (role == null || !role.equals("admin"))) {
            logger.warn("Booking read rejected due to insufficient permissions");
            throw new ForbiddenException("forbidden");
        }
        logger.info("Booking retrieved successfully");
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CreateBookingRequest body) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) {
            logger.warn("Booking update rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        MDC.put("booking_id", id.toString());
        if (body != null && body.getRoomId() != null) {
            MDC.put("room_id", body.getRoomId().toString());
        }
        logger.info("Booking update started");
        var booking = bookingService.updateBooking(userId, role, id, body);
        logger.info("Booking update completed successfully");
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        var userId = getCurrentUserId();
        var role = getCurrentUserRole();
        if (userId == null) {
            logger.warn("Booking cancellation rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }
        MDC.put("user_id", userId.toString());
        MDC.put("booking_id", id.toString());
        logger.info("Booking cancellation started");
        bookingService.cancel(id, userId, role);
        logger.info("Booking cancellation completed successfully");
        return ResponseEntity.noContent().build();
    }
}
