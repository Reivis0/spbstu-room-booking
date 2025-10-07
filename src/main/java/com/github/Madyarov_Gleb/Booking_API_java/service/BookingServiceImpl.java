package com.github.Madyarov_Gleb.Booking_API_java.service;

import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingCreateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.request.BookingUpdateRequest;
import com.github.Madyarov_Gleb.Booking_API_java.dto.response.BookingResponse;
import com.github.Madyarov_Gleb.Booking_API_java.entity.BookingEntity;
import com.github.Madyarov_Gleb.Booking_API_java.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEM_KEY_PREFIX = "idem:POST:/bookings:";

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest req, String idemKey, String userId) {
        String redisKey = IDEM_KEY_PREFIX + idemKey;
        String cachedId = redisTemplate.opsForValue().get(redisKey);
        if(cachedId != null) {
            UUID bookingId = UUID.fromString(cachedId);
            return getBooking(bookingId);
        }

        if(req.getEndsAt().isBefore(req.getStartsAt())) {
            throw new IllegalArgumentException("ends_at must be after starts_at");
        }

        BookingEntity booking = BookingEntity.builder()
                .roomId(req.getRoomId())
                .userId(userId)
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .purpose((req.getPurpose()))
                .note(req.getNote())
                .status(BookingEntity.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        bookingRepository.save(booking);
        redisTemplate.opsForValue().set(redisKey, booking.getId().toString(), 24, TimeUnit.HOURS);

        return BookingResponse.fromEntity(booking);
    }

    @Override
    public List<BookingResponse> getAllBooking() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Override
    public BookingResponse getBooking(UUID id) {
        BookingEntity entity = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        return BookingResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(UUID id, BookingUpdateRequest req) {
        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking not found"));

//        if(req.getEndsAt().isBefore(req.getStartsAt())) {
//            throw new IllegalArgumentException("ends_at must be after starts_at");
//        }

        if(req.getRoomId() != null) {
            booking.setRoomId(req.getRoomId());
        }
        if(req.getStartsAt() != null) {
            booking.setStartsAt(req.getStartsAt());
        }
        if(req.getEndsAt() != null) {
            booking.setEndsAt(req.getEndsAt());
        }
        if(req.getPurpose() != null) {
            booking.setPurpose(req.getPurpose());
        }
        if(req.getNote() != null) {
            booking.setNote(req.getNote());
        }

        booking.setUpdatedAt(OffsetDateTime.now());

        bookingRepository.save(booking);

        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional
    public void deleteBooking(UUID id) {

        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking not found"));

//        if(!booking.getUserId().equals(userId)) {
//            throw new SecurityException("You are not allowed to delete this booking");
//        }

        bookingRepository.delete(booking);
    }
}





























