package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.dto.RoomScheduleResponse;
import com.github.MadyarovGleb.booking_mvp.entity.Room;
import com.github.MadyarovGleb.booking_mvp.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) UUID building_id,
            @RequestParam(required = false) Integer capacity_min,
            @RequestParam(required = false) Integer capacity_max,
            @RequestParam(required = false) List<String> features,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String university,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime available_from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime available_to,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (building_id != null) {
            MDC.put("building_id", building_id.toString());
        }
        logger.info("Room search started unpaged={}", unpaged);
        
        if (unpaged) {
            Pageable unpagedPageable = Pageable.unpaged();
            Page<Room> rooms = roomService.search(
                    building_id, capacity_min, capacity_max, features,
                    search,
                    university,
                    available_from != null ? available_from.toString() : null,
                    available_to != null ? available_to.toString() : null,
                    unpagedPageable
            );
            logger.info("Room search completed successfully count={}", rooms.getNumberOfElements());
            return ResponseEntity.ok(rooms.getContent());
        } else {
            Page<Room> rooms = roomService.search(
                    building_id, capacity_min, capacity_max, features,
                    search,
                    university,
                    available_from != null ? available_from.toString() : null,
                    available_to != null ? available_to.toString() : null,
                    pageable
            );
            logger.info("Room search completed successfully count={}", rooms.getNumberOfElements());
            return ResponseEntity.ok(rooms);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> get(@PathVariable UUID id) {
        MDC.put("room_id", id.toString());
        logger.info("Room metadata request started");
        Room room = roomService.getById(id);
        logger.info("Room metadata request completed successfully");
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> availability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam(defaultValue = "30") int min_duration,
            @RequestParam(required = false) String university
    ) {
        MDC.put("room_id", id.toString());
        logger.info("Room availability request started date={} min_duration_minutes={}", date, min_duration);
        var slots = roomService.getAvailability(id, date, min_duration);
        logger.info("Room availability request completed successfully slots_count={}", slots.size());
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<RoomScheduleResponse> getSchedule(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam(required = false) String university
    ) {
        MDC.put("room_id", id.toString());
        logger.info("Room schedule request started roomId={} date={}", id, date);
        RoomScheduleResponse schedule = roomService.getRoomSchedule(id, date);
        logger.info("Room schedule request completed successfully");
        return ResponseEntity.ok(schedule);
    }
}
