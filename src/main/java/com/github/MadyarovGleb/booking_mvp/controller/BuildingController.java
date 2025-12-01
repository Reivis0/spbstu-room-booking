package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.service.BuildingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private final BuildingService service;

    public BuildingController(BuildingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Building>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }
    }
}
