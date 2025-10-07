package com.github.Madyarov_Gleb.Booking_API_java.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BookingUpdateRequest {

    private String roomId;

    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    private String purpose;

    private String note;
}
