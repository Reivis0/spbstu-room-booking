package com.github.Madyarov_Gleb.Booking_API_java.service;

import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingCreateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingUpdateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.response.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(BookingCreateRequest request, String idempotencyKey, String userId);
    List<BookingResponse> getAllBooking();
    BookingResponse getBooking(UUID id);
    BookingResponse updateBooking(UUID id, BookingUpdateRequest updateRequest);
    void deleteBooking(UUID id);
}
