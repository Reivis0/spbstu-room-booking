package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.repository.BuildingRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository repo;
    private final RedisService redis;

    public BuildingService(BuildingRepository repo, RedisService redis) {
        this.repo = repo;
        this.redis = redis;
    }

    public List<Building> findAll() {
        String key = "buildings:all";
        List<Building> cached = redis.get(key, List.class);
        if (cached != null) return cached;

        List<Building> buildings = repo.findAll();
        redis.set(key, buildings, Duration.ofHours(24));
        return buildings;
    }

    public Building findById(UUID id) {
        String key = "building:" + id;
        Building cached = redis.get(key, Building.class);
        if (cached != null) return cached;

        Building building = repo.findById(id).orElseThrow();
        redis.set(key, building, Duration.ofHours(24));
        return building;
    }

    public void invalidateCache(UUID buildingId) {
        redis.delete("building:" + buildingId);
        redis.delete("buildings:all");
    }
}
