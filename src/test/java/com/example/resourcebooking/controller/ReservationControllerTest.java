package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @Test
    @DisplayName("GET /reservations - Unauthenticated returns 401 Unauthorized")
    void testGetReservations_Unauthenticated() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required or invalid token"));
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"USER"})
    @DisplayName("POST /reservations - Authenticated USER can create reservation")
    void testCreateReservation_AuthenticatedUser_Success() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(start);
        request.setEndTime(end);

        ReservationResponse response = new ReservationResponse(
                10L, 1L, "test_user", 1L, "Room A",
                start, end, new BigDecimal("100.00"), ReservationStatus.PENDING, LocalDateTime.now());

        when(reservationService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.resourceName").value("Room A"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"USER"})
    @DisplayName("POST /reservations - Double booking returns 400 Bad Request")
    void testCreateReservation_DoubleBooking_Returns400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(start);
        request.setEndTime(end);

        when(reservationService.create(any(), any()))
                .thenThrow(new BadRequestException("Resource is already booked for the selected time slot"));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Resource is already booked for the selected time slot"));
    }

    @Test
    @WithMockUser(username = "unauthorized_user", roles = {"USER"})
    @DisplayName("GET /reservations/10 - Non-owner / unauthorized user returns 403 Forbidden gracefully")
    void testGetReservation_UnauthorizedUser_Returns403() throws Exception {
        mockMvc.perform(get("/reservations/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }
}
