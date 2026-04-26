package com.github.MadyarovGleb.booking_mvp.service;

import room_service.*;
import com.github.MadyarovGleb.booking_mvp.dto.RoomScheduleResponse;
import com.github.MadyarovGleb.booking_mvp.dto.TimeSlotDto;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import com.github.MadyarovGleb.booking_mvp.specification.RoomSpecification;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final AvailabilityEngineClient availability;
    private final RedisService redis;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public RoomService(RoomRepository roomRepository,
                       AvailabilityEngineClient availability,
                       RedisService redis) {
        this.roomRepository = roomRepository;
        this.availability = availability;
        this.redis = redis;
    }

    public Room getById(UUID id) {
        MDC.put("room_id", id.toString());
        try {
            String key = "room:metadata:" + id;
            Room cached = redis.get(key, Room.class);
            if (cached != null) {
                log.debug("Room metadata returned from cache");
                return cached;
            }

            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Room not found: " + id));
            redis.set(key, room, Duration.ofHours(1));
            log.info("Room metadata loaded from database and cached");
            return room;
        } finally {
            MDC.remove("room_id");
        }
    }

    public Page<Room> search(UUID buildingId, Integer capacityMin, Integer capacityMax,
                             List<String> features, String searchStr, String university, String availableFrom, String availableTo, Pageable pageable) {
        if (buildingId != null) {
            MDC.put("building_id", buildingId.toString());
        }

        // Convert frontend university code to database code
        String dbUniversityCode = com.github.MadyarovGleb.booking_mvp.util.UniversityMapper.toDbCode(university);
        
        if (dbUniversityCode != null) {
            MDC.put("university", dbUniversityCode);
            log.debug("Filtering rooms by university: {} (mapped from {})", dbUniversityCode, university);
        }

        try {
            Specification<Room> spec = RoomSpecification.withFilters(
                    buildingId, dbUniversityCode, capacityMin, capacityMax, features, searchStr
            );
            Page<Room> roomPage = roomRepository.findAll(spec, pageable);

            if (availableFrom != null && availableTo != null) {
                String date = availableFrom.substring(0, 10);
                String startTime = availableFrom.substring(11, 16);
                String endTime = availableTo.substring(11, 16);

                List<CompletableFuture<Room>> futures = roomPage.getContent().stream()
                        .map(room -> CompletableFuture.supplyAsync(() -> {
                            List<TimeSlot> intervals = availability.computeIntervals(
                                    room.getId().toString(), date, startTime, endTime
                            );
                            return intervals.isEmpty() ? null : room;
                        }, executorService))
                        .collect(Collectors.toList());

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.SECONDS);

                    List<Room> availableRooms = futures.stream()
                            .map(f -> f.getNow(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    log.info("Room search completed count={}", availableRooms.size());
                    return new PageImpl<>(availableRooms, pageable, roomPage.getTotalElements());
                } catch (Exception e) {
                    log.error("Batch availability check failed", e);
                    return new PageImpl<>(Collections.emptyList(), pageable, 0);
                }
            }

            log.info("Room search completed count={}", roomPage.getNumberOfElements());
            return roomPage;
        } finally {
            MDC.remove("building_id");
        }
    }

    public List<TimeSlotDto> getAvailability(UUID roomId, String date, int minDurationMinutes) {
        MDC.put("room_id", roomId.toString());
        try {
            String startTime = "09:00";
            String endTime = "21:00";

            List<TimeSlot> slots = availability.computeIntervals(
                    roomId.toString(), date, startTime, endTime
            );

            log.info("Room availability computed date={} slots_count={}", date, slots.size());
            return slots.stream()
                    .map(slot -> TimeSlotDto.builder()
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .build())
                    .collect(Collectors.toList());
        } finally {
            MDC.remove("room_id");
        }
    }

    public RoomScheduleResponse getRoomSchedule(UUID roomId, String dateStr) {
        MDC.put("room_id", roomId.toString());
        try {
            log.info("Room schedule request started roomId={} date={}", roomId, dateStr);

            List<AvailabilityEngineClient.OccupiedSlot> occupiedIntervals = availability.occupiedIntervals(roomId.toString(), dateStr);

            List<RoomScheduleResponse.ScheduleSlot> occupied = occupiedIntervals.stream()
                    .map(this::mapToScheduleSlot)
                    .sorted(Comparator.comparing(RoomScheduleResponse.ScheduleSlot::getFrom))
                    .collect(Collectors.toList());

            List<RoomScheduleResponse.ScheduleSlot> allSlots = fillFreeGaps(occupied);

            log.info("Room schedule computed successfully slots_count={}", allSlots.size());

            return RoomScheduleResponse.builder()
                    .date(dateStr)
                    .roomId(roomId)
                    .slots(allSlots)
                    .build();
        } finally {
            MDC.remove("room_id");
        }
    }

    private RoomScheduleResponse.ScheduleSlot mapToScheduleSlot(AvailabilityEngineClient.OccupiedSlot slot) {
        boolean isImport = slot.getBookingId().startsWith("import_");
        return RoomScheduleResponse.ScheduleSlot.builder()
                .from(slot.getStartTime())
                .to(slot.getEndTime())
                .type(isImport ? "schedule" : "booking")
                .label(slot.getNote() != null && !slot.getNote().isEmpty() ? slot.getNote() :
                        (isImport ? "Занятие" : "Забронировано"))
                .status("occupied")
                .build();
    }

    private List<RoomScheduleResponse.ScheduleSlot> fillFreeGaps(List<RoomScheduleResponse.ScheduleSlot> occupied) {
        List<RoomScheduleResponse.ScheduleSlot> allSlots = new ArrayList<>();
        String current = "08:00";
        String dayEnd = "22:00";

        for (RoomScheduleResponse.ScheduleSlot slot : occupied) {
            if (current.compareTo(slot.getFrom()) < 0) {
                allSlots.add(createFreeSlot(current, slot.getFrom()));
            }
            allSlots.add(slot);
            if (current.compareTo(slot.getTo()) < 0) {
                current = slot.getTo();
            }
        }

        if (current.compareTo(dayEnd) < 0) {
            allSlots.add(createFreeSlot(current, dayEnd));
        }
        return allSlots;
    }

    private RoomScheduleResponse.ScheduleSlot createFreeSlot(String from, String to) {
        return RoomScheduleResponse.ScheduleSlot.builder()
                .from(from).to(to).type("free").label("").status("free").build();
    }
}
