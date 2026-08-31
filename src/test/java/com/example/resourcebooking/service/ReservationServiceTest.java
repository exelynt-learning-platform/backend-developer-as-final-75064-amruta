package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationNotFoundException;
import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private User otherUser;
    private User adminUser;
    private Resource testResource;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        testUser = new User("john_doe", "john@example.com", "password", Role.USER);
        testUser.setId(1L);

        otherUser = new User("other_user", "other@example.com", "password", Role.USER);
        otherUser.setId(2L);

        adminUser = new User("admin", "admin@example.com", "password", Role.ADMIN);
        adminUser.setId(3L);

        testResource = new Resource();
        testResource.setId(10L);
        testResource.setName("Conference Room A");
        testResource.setPrice(new BigDecimal("100.00"));
        testResource.setAvailable(true);

        start = LocalDateTime.now().plusHours(1);
        end = LocalDateTime.now().plusHours(2);
    }

    @Test
    @DisplayName("Should successfully create reservation when no overlap exists")
    void testCreateReservation_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(
                eq(10L), eq(start), eq(end), eq(ReservationStatus.CANCELLED))).thenReturn(false);

        Reservation savedReservation = new Reservation();
        savedReservation.setId(100L);
        savedReservation.setUser(testUser);
        savedReservation.setResource(testResource);
        savedReservation.setStartTime(start);
        savedReservation.setEndTime(end);
        savedReservation.setPrice(testResource.getPrice());
        savedReservation.setStatus(ReservationStatus.PENDING);
        savedReservation.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(start);
        request.setEndTime(end);

        ReservationResponse response = reservationService.create(request, authentication);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Conference Room A", response.getResourceName());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when overlapping reservation exists on create")
    void testCreateReservation_ThrowsWhenOverlapping() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(
                eq(10L), eq(start), eq(end), eq(ReservationStatus.CANCELLED))).thenReturn(true);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(start);
        request.setEndTime(end);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reservationService.create(request, authentication));

        assertEquals("Resource is already booked for the selected time slot", ex.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when end time is before start time")
    void testCreateReservation_ThrowsWhenInvalidTimes() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(end);
        request.setEndTime(start); // Invalid: end before start

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reservationService.create(request, authentication));

        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw BadRequestException when resource is not available")
    void testCreateReservation_ThrowsWhenResourceUnavailable() {
        testResource.setAvailable(false);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(start);
        request.setEndTime(end);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reservationService.create(request, authentication));

        assertEquals("Resource is not available", ex.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update reservation when valid and no overlapping reservation")
    void testUpdateReservation_Success() {
        Reservation existing = new Reservation();
        existing.setId(100L);
        existing.setUser(testUser);
        existing.setResource(testResource);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setPrice(testResource.getPrice());
        existing.setStatus(ReservationStatus.PENDING);
        existing.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservationExcludingId(
                eq(10L), eq(100L), eq(start), eq(end), eq(ReservationStatus.CANCELLED))).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(existing);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(start);
        request.setEndTime(end);

        ReservationResponse response = reservationService.update(100L, request, authentication);
        assertNotNull(response);
        assertEquals(100L, response.getId());
        verify(reservationRepository).save(existing);
    }

    @Test
    @DisplayName("Should throw BadRequestException when updating to an overlapping time slot")
    void testUpdateReservation_ThrowsWhenOverlapping() {
        Reservation existing = new Reservation();
        existing.setId(100L);
        existing.setUser(testUser);
        existing.setResource(testResource);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setPrice(testResource.getPrice());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservationExcludingId(
                eq(10L), eq(100L), eq(start), eq(end), eq(ReservationStatus.CANCELLED))).thenReturn(true);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(10L);
        request.setStartTime(start);
        request.setEndTime(end);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reservationService.update(100L, request, authentication));

        assertEquals("Resource is already booked for the selected time slot", ex.getMessage());
    }

    @Test
    @DisplayName("Should deny access when another user tries to view a reservation")
    void testGetById_AccessDeniedForDifferentUser() {
        Reservation existing = new Reservation();
        existing.setId(100L);
        existing.setUser(otherUser);
        existing.setResource(testResource);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        assertThrows(
                AccessDeniedException.class,
                () -> reservationService.getById(100L, authentication));
    }

    @Test
    @DisplayName("Admin can view any reservation regardless of ownership")
    void testGetById_AdminCanAccessAnyReservation() {
        Reservation existing = new Reservation();
        existing.setId(100L);
        existing.setUser(otherUser);
        existing.setResource(testResource);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setPrice(testResource.getPrice());
        existing.setStatus(ReservationStatus.CONFIRMED);
        existing.setCreatedAt(LocalDateTime.now());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        ReservationResponse response = reservationService.getById(100L, authentication);
        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Should delete reservation when requested by owner")
    void testDelete_Success() {
        Reservation existing = new Reservation();
        existing.setId(100L);
        existing.setUser(testUser);
        existing.setResource(testResource);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        reservationService.delete(100L, authentication);
        verify(reservationRepository).delete(existing);
    }
}
