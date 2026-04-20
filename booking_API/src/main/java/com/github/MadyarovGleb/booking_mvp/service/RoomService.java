package com.github.MadyarovGleb.booking_mvp.service;

import room_service.*;
import com.github.MadyarovGleb.booking_mvp.dto.RoomScheduleResponse;
import com.github.MadyarovGleb.booking_mvp.entity.Booking;
import com.github.MadyarovGleb.booking_mvp.entity.SchedulesImport;
import com.github.MadyarovGleb.booking_mvp.repository.BookingRepository;
import com.github.MadyarovGleb.booking_mvp.repository.SchedulesImportRepository;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final AvailabilityEngineClient availability;
    private final RedisService redis;
    private final BookingRepository bookingRepository;
    private final SchedulesImportRepository schedulesImportRepository;

    public RoomService(RoomRepository roomRepository,
                       AvailabilityEngineClient availability,
                       RedisService redis,
                       BookingRepository bookingRepository,
                       SchedulesImportRepository schedulesImportRepository) {
        this.roomRepository = roomRepository;
        this.availability = availability;
        this.redis = redis;
        this.bookingRepository = bookingRepository;
        this.schedulesImportRepository = schedulesImportRepository;
    }

    public Room getById(UUID id) {
        MDC.put("room_id", id.toString());
        String key = "room:metadata:" + id;
        Room cached = redis.get(key, Room.class);
        if (cached != null) {
            logger.debug("Room metadata returned from cache");
            return cached;
        }

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found: " + id));
        redis.set(key, room, Duration.ofHours(1));
        logger.info("Room metadata loaded from database and cached");
        return room;
    }

    public List<Room> search(UUID buildingId, Integer capacityMin, Integer capacityMax,
                             List<String> features, String search, String availableFrom, String availableTo) {
        if (buildingId != null) {
            MDC.put("building_id", buildingId.toString());
        }

        List<Room> rooms = roomRepository.findAll(); // TODO: добавить фильтры по buildingId, capacity, features, search

        if (availableFrom != null && availableTo != null) {
            rooms.removeIf(room ->
                    availability.computeIntervals(
                            room.getId().toString(),
                            availableFrom.substring(0, 10), // дата YYYY-MM-DD
                            availableFrom.substring(11, 16), // startTime HH:mm
                            availableTo.substring(11, 16) // endTime HH:mm
                    ).isEmpty()
            );
        }

        logger.info("Room search completed count={}", rooms.size());
        return rooms;
    }

    public List<TimeSlot> getAvailability(UUID roomId, String date, int minDurationMinutes) {
        MDC.put("room_id", roomId.toString());

        String startTime = "09:00";
        String endTime = "21:00";

        List<TimeSlot> slots = availability.computeIntervals(
                roomId.toString(),
                date,
                startTime,
                endTime
        );

        logger.info("Room availability computed date={} slots_count={} min_duration_minutes={}",
                date, slots.size(), minDurationMinutes);
        return slots;
    }

    public RoomScheduleResponse getRoomSchedule(UUID roomId, String dateStr) {
        MDC.put("room_id", roomId.toString());
        logger.info("Room schedule request started roomId={} date={}", roomId, dateStr);

        LocalDate date = LocalDate.parse(dateStr);
        ZoneId zoneId = ZoneId.of("Europe/Moscow");

        OffsetDateTime from = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        List<SchedulesImport> imported = schedulesImportRepository.findByRoomIdAndDate(roomId, from, to);
        List<Booking> bookings = bookingRepository.findInInterval(roomId, from, to);

        List<RoomScheduleResponse.ScheduleSlot> occupied = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (SchedulesImport s : imported) {
            occupied.add(RoomScheduleResponse.ScheduleSlot.builder()
                    .from(s.getStartsAt().atZoneSameInstant(zoneId).format(timeFormatter))
                    .to(s.getEndsAt().atZoneSameInstant(zoneId).format(timeFormatter))
                    .type("schedule")
                    .label(s.getSubject() != null ? s.getSubject() : "Занятие")
                    .status("occupied")
                    .build());
        }

        for (Booking b : bookings) {
            occupied.add(RoomScheduleResponse.ScheduleSlot.builder()
                    .from(b.getStartsAt().atZoneSameInstant(zoneId).format(timeFormatter))
                    .to(b.getEndsAt().atZoneSameInstant(zoneId).format(timeFormatter))
                    .type("booking")
                    .label("Забронировано")
                    .status("occupied")
                    .build());
        }

        occupied.sort(Comparator.comparing(RoomScheduleResponse.ScheduleSlot::getFrom));

        List<RoomScheduleResponse.ScheduleSlot> allSlots = new ArrayList<>();
        String dayStart = "08:00";
        String dayEnd = "22:00";
        String current = dayStart;

        for (RoomScheduleResponse.ScheduleSlot slot : occupied) {
            if (current.compareTo(slot.getFrom()) < 0) {
                allSlots.add(RoomScheduleResponse.ScheduleSlot.builder()
                        .from(current)
                        .to(slot.getFrom())
                        .type("free")
                        .label("")
                        .status("free")
                        .build());
            }
            allSlots.add(slot);
            if (current.compareTo(slot.getTo()) < 0) {
                current = slot.getTo();
            }
        }

        if (current.compareTo(dayEnd) < 0) {
            allSlots.add(RoomScheduleResponse.ScheduleSlot.builder()
                    .from(current)
                    .to(dayEnd)
                    .type("free")
                    .label("")
                    .status("free")
                    .build());
        }

        logger.info("Room schedule computed successfully roomId={} date={} slots_count={}",
                roomId, dateStr, allSlots.size());

        return RoomScheduleResponse.builder()
                .date(dateStr)
                .roomId(roomId)
                .slots(allSlots)
                .build();
    }
}
