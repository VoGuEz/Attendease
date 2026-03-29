package com.attendease.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenProvider {
    private final String secret;
    private final long validityInMilliseconds = 86400000; // 24h

    public JwtTokenProvider() {
        this.secret = System.getenv().getOrDefault("JWT_SECRET", "placeholder_secret_key_32_chars_minimum");
    }

    public String generateToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }
}