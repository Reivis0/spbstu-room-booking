package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.repository.BuildingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository repo;

    public BuildingService(BuildingRepository repo) {
        this.repo = repo;
    }

    public List<Building> findAll() {
        return repo.findAll();
    }

    public Building findById(UUID id) {
        return repo.findById(id).orElseThrow();
    }
}
