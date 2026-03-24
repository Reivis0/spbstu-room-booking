package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.config.JwtUtil;
import com.github.MadyarovGleb.booking_mvp.dto.AuthRequests;
import com.github.MadyarovGleb.booking_mvp.service.AuthService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequests req) {
        logger.info("Login requested");
        var u = authService.authenticate(req.getEmail(), req.getPassword());
        if (u == null) {
            logger.warn("Login failed due to invalid credentials");
            return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        }
        MDC.put("user_id", u.getId().toString());
        var accessToken = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
        var refreshToken = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
        logger.info("Login successful");
        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "profile", Map.of(
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "id", u.getId()
                )
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null) {
            logger.warn("Refresh token is missing in request");
            return ResponseEntity.badRequest().body(Map.of("error","missing_refresh_token"));
        }

        try {
            var claims = jwtUtil.parse(refreshToken);
            UUID userId = UUID.fromString(claims.getBody().getSubject());
            String email = claims.getBody().get("email", String.class);
            String role = claims.getBody().get("role", String.class);

            var newAccessToken = jwtUtil.generate(userId, email, role);
            MDC.put("user_id", userId.toString());
            logger.info("Access token refreshed successfully");
            return ResponseEntity.ok(Map.of("access_token", newAccessToken));
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Refresh token validation failed reason={}", e.getClass().getSimpleName());
            return ResponseEntity.status(401).body(Map.of("error","invalid_refresh_token"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("Profile request rejected because user is not authenticated");
            return ResponseEntity.status(401).build();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UUID)) {
            logger.warn("Profile request rejected due to invalid authentication principal");
            return ResponseEntity.status(401).build();
        }
        UUID userId = (UUID) principal;
        MDC.put("user_id", userId.toString());

        var userOpt = authService.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("Profile request failed because user was not found");
            return ResponseEntity.status(404).build();
        }

        var user = userOpt.get();
        logger.info("Profile retrieved successfully");
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", maskEmail(user.getEmail()),
                "firstname", user.getFirstname(),
                "lastname", user.getLastname(),
                "role", user.getRole().name()
        ));
    }

    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 1) return "***" + email.substring(at);
        return email.substring(0,1) + "***" + email.substring(at);
    }
}
