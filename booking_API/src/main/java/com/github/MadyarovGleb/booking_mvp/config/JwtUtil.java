package com.github.MadyarovGleb.booking_mvp.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {
    private final Key key;
    private final String issuer;
    private final long ttl;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.issuer}") String issuer,
                   @Value("${app.jwt.access-ttl-seconds}") long ttl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public String generate(UUID userId, String email, String role) {
        var now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttl)))
                .addClaims(Map.of("email", email, "role", role))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}

