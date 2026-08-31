package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.LoginRequest;
import com.example.resourcebooking.dto.LoginResponse;
import com.example.resourcebooking.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service handling user authentication and JWT token generation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates user credentials and generates a signed JWT token upon success.
     *
     * @param request the login request credentials
     * @return the login response containing the JWT token, username, and assigned role
     */
    public LoginResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String token =
                jwtService.generateToken(userDetails);

        String role =
                userDetails.getAuthorities()
                        .stream()
                        .filter(authority ->
                                authority.getAuthority()
                                        .startsWith("ROLE_"))
                        .findFirst()
                        .map(GrantedAuthority::getAuthority)
                        .map(authority ->
                                authority.replace("ROLE_", ""))
                        .orElse("");

        log.info("User '{}' successfully authenticated with role '{}'", userDetails.getUsername(), role);

        return new LoginResponse(
                token,
                userDetails.getUsername(),
                role
        );
    }
}