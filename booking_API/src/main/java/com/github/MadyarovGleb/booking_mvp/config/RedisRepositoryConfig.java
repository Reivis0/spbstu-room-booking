package com.github.MadyarovGleb.booking_mvp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.github.MadyarovGleb.booking_mvp.repository.redis")
public class RedisRepositoryConfig {
}
