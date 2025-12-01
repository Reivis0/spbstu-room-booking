package com.github.MadyarovGleb.booking_mvp.service.availability;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import room_service.RoomServiceOuterClass.Conflict;
import room_service.RoomServiceOuterClass.TimeSlot;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Primary // Говорим Spring использовать этот бин вместо настоящего AvailabilityEngineClient
public class AvailabilityEngineClientMock extends AvailabilityEngineClient {

    public AvailabilityEngineClientMock() {
        super("localhost", 50051); // Параметры не используются, но вызываем конструктор базового класса
    }

    @Override
    public List<TimeSlot> computeIntervals(String roomId, String date) {
        // Возвращаем один фиктивный свободный слот
        TimeSlot slot = TimeSlot.newBuilder()
                .setStartTime("09:00")
                .setEndTime("18:00")
                .build();
        return List.of(slot);
    }

    @Override
    public List<Conflict> validate(UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeBookingId) {
        // Всегда возвращаем пустой список конфликтов
        return new ArrayList<>();
    }
}
