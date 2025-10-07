package com.github.Madyarov_Gleb.Booking_API_java.dto.response;

import com.github.Madyarov_Gleb.Booking_API_java.entity.BookingEntity;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {

    private UUID id;
    private String roomId;
    private String userId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String status;
    private String purpose;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static BookingResponse fromEntity(BookingEntity e) {
        return BookingResponse.builder()
                .id(e.getId())
                .roomId(e.getRoomId())
                .userId(e.getUserId())
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .status(e.getStatus().name().toLowerCase())
                .purpose(e.getPurpose())
                .note(e.getNote())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
