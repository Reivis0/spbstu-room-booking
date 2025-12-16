package com.github.MadyarovGleb.booking_mvp.service;

import room_service.*;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

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
        String key = "room:metadata:" + id;
        Room cached = redis.get(key, Room.class);
        if (cached != null) return cached;

        Room room = roomRepository.findById(id).orElseThrow();
        redis.set(key, room, Duration.ofHours(1));
        return room;
    }

    public List<Room> search(UUID buildingId, Integer capacityMin, Integer capacityMax,
                             List<String> features, String search, String availableFrom, String availableTo) {

        String cacheKey = String.format("rooms:search:%s:%s:%s:%s:%s:%s",
                buildingId, capacityMin, capacityMax, features, search, availableFrom + "-" + availableTo);
        List<Room> cached = redis.get(cacheKey, List.class);
        if (cached != null) return cached;

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
        return rooms;
    }

    public List<TimeSlot> getAvailability(UUID roomId, String date, int minDurationMinutes) {
        String cacheKey = String.format("availability:%s:%s", roomId, date);
        List<TimeSlot> cached = redis.get(cacheKey, List.class);
        if (cached != null) return cached;

        String startTime = "09:00";
        String endTime = "21:00";

        List<TimeSlot> slots = availability.computeIntervals(
                roomId.toString(),
                date,
                startTime,
                endTime
        );

        redis.set(cacheKey, slots, Duration.ofMinutes(5));
        return slots;
    }
}
