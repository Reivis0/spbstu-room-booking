package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.service.BuildingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private static final Logger logger = LoggerFactory.getLogger(BuildingController.class);

    private final BuildingService service;

    public BuildingController(BuildingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Building>> list(
            @RequestParam(required = false) String university,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        logger.info("Building list request started");
        Page<Building> buildings = service.findAll(pageable);
        logger.info("Building list request completed successfully count={}", buildings.getNumberOfElements());
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Building> get(@PathVariable UUID id) {
        MDC.put("building_id", id.toString());
        logger.info("Building metadata request started");
        Building building = service.findById(id);
        logger.info("Building metadata request completed successfully");
        return ResponseEntity.ok(building);
    }
}
