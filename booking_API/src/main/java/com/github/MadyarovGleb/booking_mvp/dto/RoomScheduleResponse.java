package com.github.MadyarovGleb.booking_mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomScheduleResponse {
    private String date;
    private UUID roomId;
    private List<ScheduleSlot> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSlot {
        private String from;
        private String to;
        private String type; // "schedule" | "booking" | "free"
        private String label;
        private String status; // "occupied" | "free"
    }
}
