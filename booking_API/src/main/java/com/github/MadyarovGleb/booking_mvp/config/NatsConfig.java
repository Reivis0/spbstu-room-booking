package com.github.MadyarovGleb.booking_mvp.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {

    private static final Logger logger = LoggerFactory.getLogger(NatsConfig.class);

    @Value("${nats.url}")
    private String url;

    @Bean
    public Connection natsConnection() throws Exception {
        logger.info("Initializing NATS connection");
        Connection connection = Nats.connect(url);
        logger.info("NATS connection initialized successfully");
        return connection;
    }
}
