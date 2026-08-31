package com.example.resourcebooking.security;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSecurityServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReservationSecurityService reservationSecurityService;

    private User owner;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        owner = new User("alice", "alice@example.com", "pass", Role.USER);
        owner.setId(1L);

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setUser(owner);
    }

    @Test
    @DisplayName("isOwner returns true when authenticated username matches owner")
    void testIsOwner_TrueWhenOwner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alice");
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        boolean isOwner = reservationSecurityService.isOwner(100L, authentication);
        assertTrue(isOwner);
    }

    @Test
    @DisplayName("isOwner returns false when authenticated username does not match owner")
    void testIsOwner_FalseWhenNotOwner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("bob");
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        boolean isOwner = reservationSecurityService.isOwner(100L, authentication);
        assertFalse(isOwner);
    }

    @Test
    @DisplayName("isOwner returns false when reservation not found or unauthenticated")
    void testIsOwner_FalseWhenNotFoundOrUnauth() {
        assertFalse(reservationSecurityService.isOwner(999L, null));

        when(authentication.isAuthenticated()).thenReturn(false);
        assertFalse(reservationSecurityService.isOwner(100L, authentication));
    }
}
