package com.github.MadyarovGleb.booking_mvp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@Schema(description = "Запрос на создание/обновление бронирования")
public class CreateBookingRequest {
    @Schema(description = "UUID аудитории", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID roomId;
    
    @Schema(description = "Начало бронирования (ISO 8601)", example = "2026-05-12T10:00:00+03:00")
    private OffsetDateTime startsAt;
    
    @Schema(description = "Конец бронирования (ISO 8601)", example = "2026-05-12T11:30:00+03:00")
    private OffsetDateTime endsAt;
    
    @Schema(description = "Заголовок/Название бронирования", example = "Семинар по БД")
    private String title;
    
    @Schema(description = "ID цепочки (опционально, устанавливается системой)", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID chainId;
}
