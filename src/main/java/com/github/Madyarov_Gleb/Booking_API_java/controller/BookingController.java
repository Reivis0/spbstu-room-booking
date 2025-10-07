package com.github.Madyarov_Gleb.Booking_API_java.controller;

import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingCreateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingUpdateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.response.BookingResponse;
import com.github.Madyarov_Gleb.Booking_API_java.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.print.Book;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/booking/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request, @RequestHeader("Idempotency-Key") String idemKey) {
        String userId = "user-amogus";
        return ResponseEntity.ok(bookingService.createBooking(request, idemKey, userId));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBooking() {
        return ResponseEntity.ok(bookingService.getAllBooking());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable UUID id, @Valid @RequestBody BookingUpdateRequest updateRequest) {
        return ResponseEntity.ok(bookingService.updateBooking(id, updateRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BookingResponse> deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
