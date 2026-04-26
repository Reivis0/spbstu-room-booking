package com.github.MadyarovGleb.booking_mvp.config;

import brave.Tracing;
import brave.grpc.GrpcTracing;
import com.github.MadyarovGleb.booking_mvp.service.availability.AvailabilityEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class AvailabilityEngineConfig {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityEngineConfig.class);

    @Value("${availability-engine.host}")
    private String host;

    @Value("${availability-engine.port}")
    private int port;

    @Bean
    public AvailabilityEngineClient availabilityEngineClient(Tracing tracing) throws InterruptedException {
        logger.info("Initializing Availability Engine client with tracing");
        GrpcTracing grpcTracing = GrpcTracing.create(tracing);
        AvailabilityEngineClient client = new AvailabilityEngineClient(host, port, 
                Collections.singletonList(grpcTracing.newClientInterceptor()));
        logger.info("Availability Engine client initialized successfully");
        return client;
    }
}
