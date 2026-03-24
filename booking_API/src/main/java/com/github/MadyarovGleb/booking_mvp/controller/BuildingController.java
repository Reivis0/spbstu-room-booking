package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.service.BuildingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<Building>> list() {
        logger.info("Building list request started");
        List<Building> buildings = service.findAll();
        logger.info("Building list request completed successfully count={}", buildings.size());
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
