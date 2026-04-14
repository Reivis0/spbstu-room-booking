package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.User;
import com.github.MadyarovGleb.booking_mvp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RedisService redis;

    public AuthService(UserRepository userRepository, RedisService redis) {
        this.userRepository = userRepository;
        this.redis = redis;
    }

    @PostConstruct
    public void init() {
        logger.info("AuthService initialization started");
        if (userRepository.findByEmail("student@arch.local").isEmpty()) {
            var u = User.builder()
                    .email("student@arch.local")
                    .passwordHash(encoder.encode("password"))
                    .firstname("Student")
                    .lastname("One")
                    .role(User.Role.student)
                    .build();
            userRepository.save(u);
            logger.info("Default student user created");
        }
        if (userRepository.findByEmail("admin@arch.local").isEmpty()) {
            var a = User.builder()
                    .email("admin@arch.local")
                    .passwordHash(encoder.encode("adminpass"))
                    .firstname("Admin")
                    .lastname("Root")
                    .role(User.Role.admin)
                    .build();
            userRepository.save(a);
            logger.info("Default admin user created");
        }
        logger.info("AuthService initialization completed");
    }

    public Optional<User> findById(UUID id) {
        MDC.put("user_id", id.toString());
        logger.debug("Fetching user by id");
        return userRepository.findById(id);
    }

    public User authenticate(String email, String password) {
        var uOpt = userRepository.findByEmail(email);
        if (uOpt.isEmpty()) {
            logger.warn("Authentication failed: user not found");
            return null;
        }
        var u = uOpt.get();
        if (!encoder.matches(password, u.getPasswordHash())) {
            logger.warn("Authentication failed: invalid credentials");
            return null;
        }
        MDC.put("user_id", u.getId().toString());

        String sessionKey = "session:" + u.getId();
        redis.set(sessionKey, u, Duration.ofHours(24));

        logger.info("Authentication succeeded and session cached");
        return u;
    }

    public User getUserFromSession(UUID userId) {
        MDC.put("user_id", userId.toString());
        User user = redis.get("session:" + userId, User.class);
        if (user != null) {
            logger.debug("User loaded from session cache");
            return user;
        }
        logger.debug("Session cache miss, loading user from database");

        Optional<User> uOpt = userRepository.findById(userId);
        uOpt.ifPresent(u -> redis.set("session:" + u.getId(), u, Duration.ofHours(24)));
        if (uOpt.isPresent()) {
            logger.info("User loaded from database and cached in session");
        } else {
            logger.warn("User not found while resolving session");
        }
        return uOpt.orElse(null);
    }

    public void invalidateSession(UUID userId) {
        MDC.put("user_id", userId.toString());
        redis.delete("session:" + userId);
        logger.info("Session invalidated");
    }
}
