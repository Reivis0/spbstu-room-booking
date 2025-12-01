package com.github.MadyarovGleb.booking_mvp.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {

    @Value("${nats.url}")
    private String url;

    @Bean
    public Connection natsConnection() throws Exception {
        return Nats.connect(url);
    }
}
