package com.github.MadyarovGleb.booking_mvp.service.availability;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import room_service.*;
import room_service.RoomServiceGrpc;
import room_service.RoomServiceOuterClass.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class AvailabilityEngineClient {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityEngineClient.class);

    private final int port;
    private final List<String> candidateHosts;
    private int currentHostIndex;
    private ManagedChannel channel;
    private RoomServiceGrpc.RoomServiceBlockingStub stub;

    public AvailabilityEngineClient(String host, int port) throws InterruptedException {
        this.port = port;
        this.candidateHosts = buildCandidateHosts(host);
        this.currentHostIndex = 0;

        int retries = 5;
        while (true) {
            try {
                connectToHost(candidateHosts.get(currentHostIndex));
                logger.info("Availability Engine client initialized host={} port={}",
                        candidateHosts.get(currentHostIndex), port);
                break;
            } catch (Exception e) {
                if (--retries == 0) {
                    logger.error("Failed to initialize Availability Engine client hosts={} port={} retries_exhausted=true",
                            candidateHosts, port, e);
                    throw e;
                }
                logger.warn("Failed to initialize Availability Engine client hosts={} retries_left={}",
                        candidateHosts, retries);
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
        ComputeIntervalsResponse response = executeWithFailover(
                s -> s.computeIntervals(request),
                "compute_intervals"
        );
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
        ValidateResponse response = executeWithFailover(
                s -> s.validate(request),
                "validate"
        );
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

        OccupiedIntervalsResponse response = executeWithFailover(
                s -> s.occupiedIntervals(request),
                "occupied_intervals"
        );
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

    private synchronized void connectToHost(String host) {
        if (channel != null) {
            channel.shutdownNow();
        }
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = RoomServiceGrpc.newBlockingStub(channel);
    }

    private synchronized boolean switchToNextHost() {
        if (currentHostIndex + 1 >= candidateHosts.size()) {
            return false;
        }
        currentHostIndex++;
        String nextHost = candidateHosts.get(currentHostIndex);
        connectToHost(nextHost);
        logger.warn("Switched Availability Engine host to {} due to name resolution failure", nextHost);
        return true;
    }

    private <T> T executeWithFailover(Function<RoomServiceGrpc.RoomServiceBlockingStub, T> action, String operation) {
        StatusRuntimeException lastResolutionException = null;
        int remainingHosts = candidateHosts.size() - currentHostIndex;

        for (int i = 0; i < remainingHosts; i++) {
            try {
                return action.apply(stub);
            } catch (StatusRuntimeException e) {
                if (isNameResolutionIssue(e)) {
                    lastResolutionException = e;
                    if (!switchToNextHost()) {
                        break;
                    }
                    continue;
                }
                throw e;
            }
        }

        if (lastResolutionException != null) {
            throw lastResolutionException;
        }
        throw new RuntimeException("Availability Engine operation failed: " + operation);
    }

    private boolean isNameResolutionIssue(StatusRuntimeException ex) {
        if (ex.getStatus().getCode() != Status.Code.UNAVAILABLE) {
            return false;
        }
        String message = ex.getMessage();
        return message != null && (
                message.contains("Unable to resolve host")
                        || message.contains("UnknownHostException")
        );
    }

    private List<String> buildCandidateHosts(String configuredHost) {
        LinkedHashSet<String> hosts = new LinkedHashSet<>();
        if (configuredHost != null && !configuredHost.isBlank()) {
            hosts.add(configuredHost);
            hosts.add(configuredHost.replace("-", ""));
        }
        hosts.add("availabilityengine");
        return new ArrayList<>(hosts);
    }
}
