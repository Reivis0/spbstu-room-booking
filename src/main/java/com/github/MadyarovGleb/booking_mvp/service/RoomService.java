package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.repository.RoomRepository;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final AvailabilityEngineClient availability;

    public RoomService(RoomRepository roomRepository,
                       AvailabilityEngineClient availability) {
        this.roomRepository = roomRepository;
        this.availability = availability;
    }

    // ───────────────────────────────────────────────
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД — заглушка для кэша
    private <T> T getFromCache(String key) {
        return null; // Redis не подключён
    }

    private void putToCache(String key, Object value, int minutes) {
        // Redis не подключён — ничего не делаем
    }
    // ───────────────────────────────────────────────

    public Room getById(UUID id) {
        return roomRepository.findById(id).orElseThrow();
    }

    public List<Room> search(UUID buildingId, Integer capacityMin, Integer capacityMax,
                             List<String> features, String search, String availableFrom, String availableTo) {

        String cacheKey = String.format("rooms:search:%s:%s:%s:%s:%s:%s",
                buildingId, capacityMin, capacityMax, features, search, availableFrom + "-" + availableTo);

        List<Room> cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        List<Room> rooms = roomRepository.findAll(); // Здесь можно добавить фильтры

        if (availableFrom != null && availableTo != null) {
            rooms.removeIf(room -> availability.computeIntervals(room.getId().toString(), availableFrom).isEmpty());
        }

        putToCache(cacheKey, rooms, 10);
        return rooms;
    }

    public List<room_service.RoomServiceOuterClass.TimeSlot> getAvailability(UUID roomId, String date, int minDurationMinutes) {
        String cacheKey = String.format("availability:%s:%s", roomId, date);
        List<room_service.RoomServiceOuterClass.TimeSlot> cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        List<room_service.RoomServiceOuterClass.TimeSlot> slots = availability.computeIntervals(roomId.toString(), date);
        putToCache(cacheKey, slots, 10);
        return slots;
    }
}
