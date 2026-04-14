package com.github.MadyarovGleb.booking_mvp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            logger.debug("Redis cache miss for key={}", key);
            return null;
        }
        logger.debug("Redis cache hit for key={}", key);
        return (T) value;
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        logger.debug("Redis key set key={} ttl_seconds={}", key, ttl.toSeconds());
    }

    public void delete(String key) {
        redisTemplate.delete(key);
        logger.debug("Redis key deleted key={}", key);
    }

    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.debug("Redis keys deleted by pattern={} keys_count={}", pattern, keys.size());
            return;
        }
        logger.debug("Redis deletePattern found no keys for pattern={}", pattern);
    }
}
