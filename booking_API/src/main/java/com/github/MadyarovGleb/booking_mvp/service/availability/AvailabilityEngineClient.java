package com.github.MadyarovGleb.booking_mvp.service.availability;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import room_service.*;
import room_service.RoomServiceGrpc;
import room_service.RoomServiceOuterClass.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AvailabilityEngineClient {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityEngineClient.class);

    private RoomServiceGrpc.RoomServiceBlockingStub stub;

    public AvailabilityEngineClient(String host, int port) throws InterruptedException {
        int retries = 5;
        while (true) {
            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();
                stub = RoomServiceGrpc.newBlockingStub(channel);
                logger.info("Connected to Availability Engine host={} port={}", host, port);
                break;
            } catch (Exception e) {
                if (--retries == 0) {
                    logger.error("Failed to connect to Availability Engine host={} port={} retries_exhausted=true",
                            host, port, e);
                    throw e;
                }
                logger.warn("Failed to connect to Availability Engine host={} port={} retries_left={}",
                        host, port, retries);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
        }
    }

    public List<TimeSlot> computeIntervals(String roomId, String date, String startTime, String endTime) {
        ComputeIntervalsRequest.Builder builder = ComputeIntervalsRequest.newBuilder()
                .setRoomId(roomId)
                .setDate(date);

        if (startTime != null) builder.setStartTime(startTime);
        if (endTime != null) builder.setEndTime(endTime);

        ComputeIntervalsRequest request = builder.build();

        ComputeIntervalsResponse response = stub.computeIntervals(request);
        logger.debug("Availability intervals computed room_id={} date={} slots_count={}",
                roomId, date, response.getAvailableSlotsCount());

        return response.getAvailableSlotsList();
    }

    public ValidationResult validate(UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt, UUID excludeBookingId) {
        String date = startsAt.toLocalDate().toString();
        String startTime = startsAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String endTime = endsAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        ValidateRequest.Builder builder = ValidateRequest.newBuilder()
                .setRoomId(roomId.toString())
                .setDate(date)
                .setStartTime(startTime)
                .setEndTime(endTime);

        ValidateRequest request = builder.build();
        ValidateResponse response = stub.validate(request);
        logger.debug("Availability validation completed room_id={} date={} is_valid={} conflicts_count={}",
                roomId, date, response.getIsValid(), response.getConflictsCount());

        List<BookingConflict> conflicts = new ArrayList<>();
        for (Conflict c : response.getConflictsList()) {
            conflicts.add(new BookingConflict(
                    c.getBookingId(),
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getUserId()
            ));
        }

        return new ValidationResult(response.getIsValid(), response.getDetails(), conflicts);
    }

    public List<OccupiedSlot> occupiedIntervals(String roomId, String date) {
        OccupiedIntervalsRequest request = OccupiedIntervalsRequest.newBuilder()
                .setRoomId(roomId)
                .setDate(date)
                .build();

        OccupiedIntervalsResponse response = stub.occupiedIntervals(request);
        logger.debug("Occupied intervals fetched room_id={} date={} intervals_count={}",
                roomId, date, response.getIntervalsCount());

        List<OccupiedSlot> result = new ArrayList<>();
        for (OccupiedSlotProto slot : response.getIntervalsList()) {
            result.add(new OccupiedSlot(
                    slot.getBookingId(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getUserId(),
                    slot.getNote()
            ));
        }
        return result;
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final ValidationDetails details;
        private final List<BookingConflict> conflicts;

        public ValidationResult(boolean isValid, ValidationDetails details, List<BookingConflict> conflicts) {
            this.isValid = isValid;
            this.details = details;
            this.conflicts = conflicts;
        }

        public boolean isValid() { return isValid; }
        public ValidationDetails getDetails() { return details; }
        public List<BookingConflict> getConflicts() { return conflicts; }
    }

    public static class BookingConflict {
        private final String bookingId;
        private final String startTime;
        private final String endTime;
        private final String userId;

        public BookingConflict(String bookingId, String startTime, String endTime, String userId) {
            this.bookingId = bookingId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.userId = userId;
        }

        public String getBookingId() { return bookingId; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getUserId() { return userId; }
    }

    public static class OccupiedSlot {
        private final String bookingId;
        private final String startTime;
        private final String endTime;
        private final String userId;
        private final String note;

        public OccupiedSlot(String bookingId, String startTime, String endTime, String userId, String note) {
            this.bookingId = bookingId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.userId = userId;
            this.note = note;
        }

        public String getBookingId() { return bookingId; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getUserId() { return userId; }
        public String getNote() { return note; }
    }
}
