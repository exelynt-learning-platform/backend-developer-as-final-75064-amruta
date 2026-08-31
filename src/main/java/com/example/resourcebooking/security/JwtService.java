package com.example.resourcebooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

        @Value("${jwt.secret}")
        private String secret;

        @Value("${jwt.expiration}")
        private long expiration;

        @PostConstruct
        public void validateJwtConfiguration() {

                if (secret == null || secret.isBlank()) {
                        throw new IllegalStateException(
                                        "JWT_SECRET environment variable is not configured");
                }

                try {
                        byte[] keyBytes = Base64.getDecoder().decode(secret);

                        if (keyBytes.length < 32) {
                                throw new IllegalStateException(
                                                "JWT_SECRET must decode to at least 32 bytes");
                        }

                } catch (IllegalArgumentException ex) {
                        throw new IllegalStateException(
                                        "JWT_SECRET must be a valid Base64-encoded value",
                                        ex);
                }

                if (expiration <= 0) {
                        throw new IllegalStateException(
                                        "JWT expiration must be greater than 0");
                }
        }

        private SecretKey getSigningKey() {

                byte[] keyBytes = Base64.getDecoder().decode(secret);

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
