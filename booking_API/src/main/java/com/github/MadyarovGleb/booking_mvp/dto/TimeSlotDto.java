package com.github.MadyarovGleb.booking_mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
    private String startTime;
    private String endTime;
}
