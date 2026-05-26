package com.naukri.bot.security;

import com.naukri.bot.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final AppProperties properties;

    public JwtService(AppProperties properties) {
        this.properties = properties;
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of("roles", userDetails.getAuthorities().toString()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.security().tokenExpirationMinutes() * 60)))
                .signWith(secretKey())
                .compact();
    }

    public String extractSubject(String token) {
        try {
            return claims(token).getSubject();
        } catch (Exception exception) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String subject = extractSubject(token);
        return subject != null && subject.equals(userDetails.getUsername()) && claims(token).getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey secretKey() {
        String secret = properties.security().jwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
