package com.github.MadyarovGleb.booking_mvp.service.availability;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import room_service.RoomServiceGrpc;
import room_service.RoomServiceOuterClass.Conflict;
import room_service.RoomServiceOuterClass.TimeSlot;
import room_service.RoomServiceOuterClass.ValidateRequest;
import room_service.RoomServiceOuterClass.ValidateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AvailabilityEngineClient {

    private final RoomServiceGrpc.RoomServiceBlockingStub stub;

    public AvailabilityEngineClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = RoomServiceGrpc.newBlockingStub(channel);
    }

    // Возвращаем доступные интервалы
    public List<TimeSlot> computeIntervals(String roomId, String date) {
        var request = room_service.RoomServiceOuterClass.ComputeIntervalsRequest.newBuilder()
                .setRoomId(roomId)
                .setDate(date)
                .build();
        var response = stub.computeIntervals(request);
        return response.getAvailableSlotsList();
    }

    // Проверка доступности + конфликты
    public List<Conflict> validate(UUID roomId, java.time.OffsetDateTime startsAt, java.time.OffsetDateTime endsAt, UUID excludeBookingId) {
        var request = ValidateRequest.newBuilder()
                .setRoomId(roomId.toString())
                .setDate(startsAt.toLocalDate().toString())
                .setStartTime(startsAt.toLocalTime().toString())
                .setEndTime(endsAt.toLocalTime().toString())
                .build();

        ValidateResponse response = stub.validate(request);

        List<Conflict> conflicts = new ArrayList<>();
        for (Conflict c : response.getConflictsList()) {
            conflicts.add(Conflict.newBuilder()
                    .setBookingId(c.getBookingId())
                    .setStartTime(c.getStartTime())
                    .setEndTime(c.getEndTime())
                    .setStatus(c.getStatus())
                    .setUserId(c.getUserId())
                    .build());
        }
        return conflicts;
    }
}
