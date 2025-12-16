package com.github.MadyarovGleb.booking_mvp.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateBookingRequest {
    private UUID roomId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
}

