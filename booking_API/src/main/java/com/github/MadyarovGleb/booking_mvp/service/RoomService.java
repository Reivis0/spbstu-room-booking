package com.github.MadyarovGleb.booking_mvp.service;

import room_service.*;
import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final AvailabilityEngineClient availability;
    private final RedisService redis;

    public RoomService(RoomRepository roomRepository,
                       AvailabilityEngineClient availability,
                       RedisService redis) {
        this.roomRepository = roomRepository;
        this.availability = availability;
        this.redis = redis;
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

        String cacheKey = String.format("rooms:search:%s:%s:%s:%s:%s:%s",
                buildingId, capacityMin, capacityMax, features, search, availableFrom + "-" + availableTo);
        List<Room> cached = redis.get(cacheKey, List.class);
        if (cached != null) {
            logger.debug("Room search returned from cache count={}", cached.size());
            return cached;
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

        redis.set(cacheKey, rooms, Duration.ofMinutes(10));
        logger.info("Room search completed and cached count={}", rooms.size());
        return rooms;
    }

    public List<TimeSlot> getAvailability(UUID roomId, String date, int minDurationMinutes) {
        MDC.put("room_id", roomId.toString());
        String cacheKey = String.format("availability:%s:%s", roomId, date);
        List<TimeSlot> cached = redis.get(cacheKey, List.class);
        if (cached != null) {
            logger.debug("Room availability returned from cache date={} slots_count={}", date, cached.size());
            return cached;
        }

        String startTime = "09:00";
        String endTime = "21:00";

        List<TimeSlot> slots = availability.computeIntervals(
                roomId.toString(),
                date,
                startTime,
                endTime
        );

        redis.set(cacheKey, slots, Duration.ofMinutes(5));
        logger.info("Room availability computed and cached date={} slots_count={} min_duration_minutes={}",
                date, slots.size(), minDurationMinutes);
        return slots;
    }
}
