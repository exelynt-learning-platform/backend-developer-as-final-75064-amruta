package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.service.ResourceService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceService resourceService;

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("GET /api/resources - USER role can read resources")
    void testGetResources_UserRole_Allowed() throws Exception {
        ResourceResponse res = new ResourceResponse(1L, "Laptop", "Dell", "EQUIPMENT", true, new BigDecimal("300.00"));
        when(resourceService.getAll()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("POST /api/resources - USER role is forbidden from creating resources")
    void testCreateResource_UserRole_Forbidden() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("Projector");
        request.setType("EQUIPMENT");
        request.setAvailable(true);
        request.setPrice(new BigDecimal("200.00"));

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("POST /api/resources - ADMIN role can create resources")
    void testCreateResource_AdminRole_Allowed() throws Exception {
        ResourceRequest request = new ResourceRequest();
        request.setName("Projector");
        request.setType("EQUIPMENT");
        request.setAvailable(true);
        request.setPrice(new BigDecimal("200.00"));

        ResourceResponse response = new ResourceResponse(1L, "Projector", null, "EQUIPMENT", true, new BigDecimal("200.00"));
        when(resourceService.create(any(ResourceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Projector"));
    }
}
