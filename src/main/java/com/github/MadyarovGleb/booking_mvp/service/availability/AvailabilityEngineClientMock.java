package com.github.MadyarovGleb.booking_mvp.service.availability;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import room_service.RoomServiceOuterClass.*;
import room_service.ValidationDetails;
import room_service.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Primary // Spring будет использовать этот бин вместо настоящего AvailabilityEngineClient
public class AvailabilityEngineClientMock extends AvailabilityEngineClient {

    public AvailabilityEngineClientMock() {
        super("localhost", 50051); // параметры не используются, т.к. это мок
    }

    // Возвращаем фиктивные доступные слоты
    @Override
    public List<TimeSlot> computeIntervals(String roomId, String date, String startTime, String endTime) {
        TimeSlot slot1 = TimeSlot.newBuilder()
                .setStartTime(startTime != null ? startTime : "09:00")
                .setEndTime(endTime != null ? endTime : "18:00")
                .build();

        TimeSlot slot2 = TimeSlot.newBuilder()
                .setStartTime("18:00")
                .setEndTime("19:00")
                .build();

        List<TimeSlot> slots = new ArrayList<>();
        slots.add(slot1);
        slots.add(slot2);

        return slots;
    }

    // Возвращаем фиктивные конфликты (или пустой список)
    @Override
    public ValidationResult validate(UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeBookingId) {
        // Пустой список конфликтов
        List<BookingConflict> conflicts = new ArrayList<>();

        // Фиктивные детали валидации
        ValidationDetails details = ValidationDetails.newBuilder()
                .setDurationValid(true)
                .setWorkingHoursValid(true)
                .setNoConflicts(true)
                .build();

        return new ValidationResult(true, details, conflicts);
    }

    // Возвращаем фиктивные занятые слоты
    @Override
    public List<OccupiedSlot> occupiedIntervals(String roomId, String date) {
        List<OccupiedSlot> slots = new ArrayList<>();
        slots.add(new OccupiedSlot(
                UUID.randomUUID().toString(),
                "10:00",
                "11:00",
                "user-123",
                "Test booking"
        ));
        return slots;
    }
}
