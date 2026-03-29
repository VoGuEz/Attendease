package com.attendease.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenProvider {
    private final String secret;
    private final long validityInMilliseconds = 86400000; // 24 hours

    public JwtTokenProvider() {
        // Pulls the JWT_SECRET you set in Railway
        this.secret = System.getenv().getOrDefault("JWT_SECRET", "default_secret_key_at_least_32_chars_long_to_meet_security_requirements");
    }

    public String generateToken(String email) {
        // Modern Way: Use SecretKey and StandardCharsets
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        // Modern Builder: signWith no longer requires the Algorithm parameter 
        // as it is inferred from the Key type.
        return Jwts.builder()
                .subject(email) // .setSubject() is now .subject()
                .issuedAt(now)  // .setIssuedAt() is now .issuedAt()
                .expiration(validity) // .setExpiration() is now .expiration()
                .signWith(key) // Algorithm is automatically detected (HS256)
                .compact();
    }
}