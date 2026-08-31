package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.LoginRequest;
import com.example.resourcebooking.dto.LoginResponse;
import com.example.resourcebooking.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Validated @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request));
    }
}