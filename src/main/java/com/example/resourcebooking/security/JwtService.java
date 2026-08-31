package com.example.resourcebooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

/**
 * Service responsible for generating, signing, extracting claims from, and validating JWT tokens.
 * Seamlessly supports both Base64-encoded keys and UTF-8 key strings of at least 256 bits (32 bytes).
 */
@Service
public class JwtService {

    @Value("${jwt.secret:MyVeryLongSecretKeyForResourceBookingSystemJWT2026SecureKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    public void validateJwtConfiguration() {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET configuration is missing or blank");
        }

        byte[] keyBytes = resolveKeyBytes(secret);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes (256 bits)");
        }

        if (expiration <= 0) {
            throw new IllegalStateException(
                    "JWT expiration must be greater than 0");
        }
    }

    private byte[] resolveKeyBytes(String secretValue) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secretValue);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a Base64 string; fallback to UTF-8 bytes
        }
        return secretValue.getBytes(StandardCharsets.UTF_8);
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes = resolveKeyBytes(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {

        return extractClaim(
                token,
                Claims::getSubject);
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);

        return username != null
                && username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        return extractExpiration(token)
                .before(new Date());
    }

    private Date extractExpiration(String token) {

        return extractClaim(
                token,
                Claims::getExpiration);
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }
}
