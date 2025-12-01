package com.github.MadyarovGleb.booking_mvp.service;

import com.github.MadyarovGleb.booking_mvp.entity.User;
import com.github.MadyarovGleb.booking_mvp.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) { this.userRepository = userRepository; }

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("student@arch.local").isEmpty()) {
            var u = User.builder()
                    .email("student@arch.local")
                    .passwordHash(encoder.encode("password"))
                    .firstname("Student").lastname("One").role("student").build();
            userRepository.save(u);
        }
        if (userRepository.findByEmail("admin@arch.local").isEmpty()) {
            var a = User.builder()
                    .email("admin@arch.local")
                    .passwordHash(encoder.encode("adminpass"))
                    .firstname("Admin").lastname("Root").role("admin").build();
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
        return u;
    }
}

