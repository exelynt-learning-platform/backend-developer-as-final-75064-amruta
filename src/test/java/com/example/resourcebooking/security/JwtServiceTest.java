package com.example.resourcebooking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    // 256-bit Base64-encoded secret key
    private final String secret = "dGhpcy1pcy1hLXNhbXBsZS1qd3Qtc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seQ==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        jwtService.validateJwtConfiguration();
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract correct username")
    void testTokenGenerationAndExtraction() {
        UserDetails userDetails = new User("admin", "password", Collections.emptyList());

        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("admin", username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}
