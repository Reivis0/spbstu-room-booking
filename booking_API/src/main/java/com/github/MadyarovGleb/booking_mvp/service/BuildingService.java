package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.repository.BuildingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private static final Logger logger = LoggerFactory.getLogger(BuildingService.class);

    private final BuildingRepository repo;
    private final RedisService redis;

    public BuildingService(BuildingRepository repo, RedisService redis) {
        this.repo = repo;
        this.redis = redis;
    }

    public List<Building> findAll() {
        String key = "buildings:all";
        List<Building> cached = redis.get(key, List.class);
        if (cached != null) {
            logger.debug("Buildings list returned from cache count={}", cached.size());
            return cached;
        }

        List<Building> buildings = repo.findAll();
        redis.set(key, buildings, Duration.ofHours(24));
        logger.info("Buildings list loaded from database and cached count={}", buildings.size());
        return buildings;
    }

    public Building findById(UUID id) {
        MDC.put("building_id", id.toString());
        String key = "building:" + id;
        Building cached = redis.get(key, Building.class);
        if (cached != null) {
            logger.debug("Building metadata returned from cache");
            return cached;
        }

        Building building = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Building not found: " + id));
        redis.set(key, building, Duration.ofHours(24));
        logger.info("Building metadata loaded from database and cached");
        return building;
    }

    public void invalidateCache(UUID buildingId) {
        MDC.put("building_id", buildingId.toString());
        redis.delete("building:" + buildingId);
        redis.delete("buildings:all");
        logger.debug("Building cache invalidated");
    }
}
