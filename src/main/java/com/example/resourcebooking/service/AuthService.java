package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.LoginRequest;
import com.example.resourcebooking.dto.LoginResponse;
import com.example.resourcebooking.dto.RegisterRequest;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.UserRepository;
import com.example.resourcebooking.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication, registration, and JWT token generation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    /**
     * Registers a new user account with validated credentials and returns a signed JWT token.
     *
     * @param request user registration details
     * @return the login response containing the JWT token, username, and assigned role
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role
        );

        userRepository.save(user);

        log.info("User '{}' successfully registered with role '{}'", user.getUsername(), role.name());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + role.name())
                .build();

        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(
                token,
                user.getUsername(),
                role.name()
        );
    }
}