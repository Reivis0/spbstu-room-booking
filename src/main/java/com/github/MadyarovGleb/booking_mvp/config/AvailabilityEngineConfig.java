package com.github.MadyarovGleb.booking_mvp.config;

import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvailabilityEngineConfig {

    @Value("${availability-engine.host}")
    private String host;

    @Value("${availability-engine.port}")
    private int port;

    @Bean
    public AvailabilityEngineClient availabilityEngineClient() {
        return new AvailabilityEngineClient(host, port);
    }
}
