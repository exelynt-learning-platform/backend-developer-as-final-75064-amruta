package com.example.resourcebooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtService implements InitializingBean {

    private static final Set<String> INSECURE_SAMPLE_SECRETS = Set.of(
            "dGhpcy1pcy1hLXNhbXBsZS1qd3Qtc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seQ=="
    );

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey signingKey;
    private JwtParser jwtParser;

    @Override
    public void afterPropertiesSet() {
        validateJwtConfiguration();
        initializeParser();
    }

    public void validateJwtConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is not configured");
        }

        if (INSECURE_SAMPLE_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException(
                    "JWT_SECRET must not use the publicly known sample or default secret. Please configure a unique 256-bit secret.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded value", ex);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes (256 bits)");
        }

        if (expiration <= 0) {
            throw new IllegalStateException("JWT expiration must be greater than 0");
        }
    }

    private void initializeParser() {
        byte[] keyBytes = Base64.getDecoder().decode(secret.trim());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(this.signingKey)
                .build();
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            return isClaimsValid(claims, userDetails);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isClaimsValid(Claims claims, UserDetails userDetails) {
        if (claims == null || userDetails == null) {
            return false;
        }
        String username = claims.getSubject();
        Date expirationDate = claims.getExpiration();
        return username != null
                && username.equals(userDetails.getUsername())
                && expirationDate != null
                && expirationDate.after(new Date());
    }
}

