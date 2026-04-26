package com.github.MadyarovGleb.booking_mvp.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChainBookingRequest {
    private String university;
    private String purpose;
    private String title;
    private List<ChainBookingItem> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChainBookingItem {
        private UUID roomId;
        private OffsetDateTime startsAt;
        private OffsetDateTime endsAt;
    }
}
