package com.example.resourcebooking.security;

import io.jsonwebtoken.Claims;
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
    // 256-bit Base64-encoded valid test secret key
    private final String validSecret = "c29tZS1zZWN1cmUtdGVzdC1qd3Qtc2VjcmV0LWtleS0yNTYtYml0cyE=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", validSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        jwtService.afterPropertiesSet();
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

    @Test
    @DisplayName("Should extract all claims once and validate correctly")
    void testExtractAllClaimsAndClaimsValidation() {
        UserDetails userDetails = new User("admin", "password", Collections.emptyList());

        String token = jwtService.generateToken(userDetails);
        Claims claims = jwtService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("admin", claims.getSubject());
        assertNotNull(claims.getExpiration());

        assertTrue(jwtService.isClaimsValid(claims, userDetails));

        UserDetails wrongUser = new User("wrongUser", "password", Collections.emptyList());
        assertFalse(jwtService.isClaimsValid(claims, wrongUser));
        assertFalse(jwtService.isClaimsValid(null, userDetails));
    }

    @Test
    @DisplayName("Should reject publicly known sample secret")
    void testPublicSampleSecret_Rejected() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "dGhpcy1pcy1hLXNhbXBsZS1qd3Qtc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtb25seQ==");
        ReflectionTestUtils.setField(service, "expiration", 86400000L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::afterPropertiesSet);
        assertTrue(ex.getMessage().contains("publicly known sample"));
    }

    @Test
    @DisplayName("Should reject blank or weak JWT secret")
    void testInvalidSecret_Rejected() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "");
        ReflectionTestUtils.setField(service, "expiration", 86400000L);

        assertThrows(IllegalStateException.class, service::afterPropertiesSet);

        ReflectionTestUtils.setField(service, "secret", "dG9vc2hvcnQ="); // Decodes to < 32 bytes
        assertThrows(IllegalStateException.class, service::afterPropertiesSet);
    }
}

