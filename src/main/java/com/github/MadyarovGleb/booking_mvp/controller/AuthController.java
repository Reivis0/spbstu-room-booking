package com.github.MadyarovGleb.booking_mvp.controller;

import com.github.MadyarovGleb.booking_mvp.config.JwtUtil;
import com.github.MadyarovGleb.booking_mvp.dto.AuthRequests;
import com.github.MadyarovGleb.booking_mvp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequests req) {
        var u = authService.authenticate(req.getEmail(), req.getPassword());
        if (u == null) return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        var accessToken = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
        var refreshToken = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
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
        if (refreshToken == null) return ResponseEntity.badRequest().body(Map.of("error","missing_refresh_token"));

        try {
            var claims = jwtUtil.parse(refreshToken);
            UUID userId = UUID.fromString(claims.getBody().getSubject());
            String email = claims.getBody().get("email", String.class);
            String role = claims.getBody().get("role", String.class);

            var newAccessToken = jwtUtil.generate(userId, email, role);
            return ResponseEntity.ok(Map.of("access_token", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error","invalid_refresh_token"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UUID)) {
            return ResponseEntity.status(401).build();
        }
        UUID userId = (UUID) principal;

        var userOpt = authService.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).build();

        var user = userOpt.get();
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
