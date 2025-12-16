package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> search(
            @RequestParam(required = false) UUID building_id,
            @RequestParam(required = false) Integer capacity_min,
            @RequestParam(required = false) Integer capacity_max,
            @RequestParam(required = false) List<String> features,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime available_from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime available_to
    ) {
        List<Room> rooms = roomService.search(
                building_id, capacity_min, capacity_max, features,
                search,
                available_from != null ? available_from.toString() : null,
                available_to != null ? available_to.toString() : null
        );
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(roomService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> availability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam(defaultValue = "30") int min_duration
    ) {
        try {
            var slots = roomService.getAvailability(id, date, min_duration);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
