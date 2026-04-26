package com.github.MadyarovGleb.booking_mvp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.github.MadyarovGleb.booking_mvp.repository")
public class JpaConfig {
}
