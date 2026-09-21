package com.surakshafin.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${surakshafin.jwt.secret}") String secret,
                       @Value("${surakshafin.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.isBlank() || secret.chars().allMatch(c -> c == '*')
                || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "SURAKSHAFIN_JWT_SECRET is missing or too weak. Set it to at least " + MIN_SECRET_BYTES +
                            " random bytes before starting the app, e.g.: export SURAKSHAFIN_JWT_SECRET=$(openssl rand -hex 32)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(Long userId, String phoneNumber, boolean admin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phoneNumber", phoneNumber)
                .claim("role", admin ? "ADMIN" : "USER")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? "USER" : role.toString();
    }

    public long remainingValidityMillis(String token) {
        Date expiry = parseClaims(token).getExpiration();
        return Math.max(0, expiry.getTime() - System.currentTimeMillis());
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
