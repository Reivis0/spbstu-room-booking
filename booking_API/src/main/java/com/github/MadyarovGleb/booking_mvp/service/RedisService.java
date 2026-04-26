package com.github.MadyarovGleb.booking_mvp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("Redis cache miss for key={}", key);
            return null;
        }
        log.debug("Redis cache hit for key={}", key);
        return (T) value;
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Redis key set key={} ttl_seconds={}", key, ttl.toSeconds());
    }

    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("Redis key deleted key={}", key);
    }

    public void deletePattern(String pattern) {
        log.debug("Deleting keys by pattern using SCAN: {}", pattern);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        
        try (Cursor<byte[]> cursor = Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection().scan(options)) {
            
            List<byte[]> keysToDelete = new ArrayList<>();
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
                if (keysToDelete.size() >= 100) {
                    performBatchDelete(keysToDelete);
                }
            }
            performBatchDelete(keysToDelete);
            log.info("Finished deleting keys for pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to delete keys by pattern: {}", pattern, e);
        }
    }

    private void performBatchDelete(List<byte[]> keys) {
        if (keys.isEmpty()) return;
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection().del(keys.toArray(new byte[0][0]));
        keys.clear();
    }
}
