package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.User;
import com.github.MadyarovGleb.booking_mvp.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final RedisService redis;

    public AuthService(UserRepository userRepository, RedisService redis) {
        this.userRepository = userRepository;
        this.redis = redis;
    }

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("student@arch.local").isEmpty()) {
            var u = User.builder()
                    .email("student@arch.local")
                    .passwordHash(encoder.encode("password"))
                    .firstname("Student")
                    .lastname("One")
                    .role(User.Role.student)
                    .build();
            userRepository.save(u);
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
        }
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public User authenticate(String email, String password) {
        var uOpt = userRepository.findByEmail(email);
        if (uOpt.isEmpty()) return null;
        var u = uOpt.get();
        if (!encoder.matches(password, u.getPasswordHash())) return null;

        String sessionKey = "session:" + u.getId();
        redis.set(sessionKey, u, Duration.ofHours(24));

        return u;
    }

    public User getUserFromSession(UUID userId) {
        User user = redis.get("session:" + userId, User.class);
        if (user != null) return user;

        Optional<User> uOpt = userRepository.findById(userId);
        uOpt.ifPresent(u -> redis.set("session:" + u.getId(), u, Duration.ofHours(24)));
        return uOpt.orElse(null);
    }

    public void invalidateSession(UUID userId) {
        redis.delete("session:" + userId);
    }
}
