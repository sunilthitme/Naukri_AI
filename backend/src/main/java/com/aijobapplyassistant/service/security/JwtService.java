package com.aijobapplyassistant.service.security;

import com.aijobapplyassistant.config.ApplicationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final ApplicationProperties properties;

    public JwtService(ApplicationProperties properties) {
        this.properties = properties;
    }

    public String generateToken(String subject) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(properties.getSecurity().getJwtExpirationMinutes());
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(getSigningKey())
                .compact();
    }

    public LocalDateTime getExpiry(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder()
                .encodeToString(properties.getSecurity().getJwtSecret().getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
