package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.LoginRequest;
import com.example.resourcebooking.dto.LoginResponse;
import com.example.resourcebooking.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * REST controller for authentication endpoints (user login and JWT generation).
 */
@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user credentials and returns a signed JWT bearer token.
     *
     * @param request login credentials
     * @return authentication response containing the token and user metadata
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request));
    }
}