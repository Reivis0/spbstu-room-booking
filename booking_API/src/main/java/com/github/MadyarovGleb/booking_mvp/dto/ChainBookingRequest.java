package com.github.MadyarovGleb.booking_mvp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@Schema(description = "Запрос на создание цепочки бронирований")
public class ChainBookingRequest {
    @Schema(description = "Код университета", example = "SPBSTU")
    private String university;
    
    @Schema(description = "Цель бронирования", example = "Учебные занятия")
    private String purpose;
    
    @Schema(description = "Заголовок цепочки", example = "Курс по алгоритмам")
    private String title;
    
    @Schema(description = "Список бронируемых интервалов")
    private List<ChainBookingItem> items;

    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @Builder
    @Schema(description = "Элемент цепочки бронирования")
    public static class ChainBookingItem {
        @Schema(description = "UUID аудитории")
        private UUID roomId;
        
        @Schema(description = "Начало интервала", example = "2026-05-12T10:00:00+03:00")
        private OffsetDateTime startsAt;
        
        @Schema(description = "Конец интервала", example = "2026-05-12T11:30:00+03:00")
        private OffsetDateTime endsAt;
    }
}
