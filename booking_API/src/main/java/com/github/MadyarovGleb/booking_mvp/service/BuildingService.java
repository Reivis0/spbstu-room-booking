package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.exception.NotFoundException;
import com.github.MadyarovGleb.booking_mvp.entity.Building;
import com.github.MadyarovGleb.booking_mvp.repository.BuildingRepository;
import com.github.MadyarovGleb.booking_mvp.specification.BuildingSpecification;
import com.github.MadyarovGleb.booking_mvp.util.UniversityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public Page<Building> findAll(String university, Pageable pageable) {
        // Convert frontend university code to database code
        String dbUniversityCode = UniversityMapper.toDbCode(university);
        
        if (dbUniversityCode != null) {
            MDC.put("university", dbUniversityCode);
            logger.debug("Filtering buildings by university: {} (mapped from {})", dbUniversityCode, university);
        }
        
        // Apply JPA Specification filter
        Specification<Building> spec = BuildingSpecification.withFilters(dbUniversityCode);
        Page<Building> buildings = repo.findAll(spec, pageable);
        
        logger.info("Buildings page loaded from database count={}", buildings.getNumberOfElements());
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
