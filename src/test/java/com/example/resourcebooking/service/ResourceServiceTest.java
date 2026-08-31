package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.repository.ResourceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = new Resource();
        resource.setId(1L);
        resource.setName("Projector");
        resource.setDescription("4K Projector");
        resource.setType("EQUIPMENT");
        resource.setAvailable(true);
        resource.setPrice(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Should create resource successfully")
    void testCreateResource() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceRequest request = new ResourceRequest();
        request.setName("Projector");
        request.setDescription("4K Projector");
        request.setType("EQUIPMENT");
        request.setAvailable(true);
        request.setPrice(new BigDecimal("150.00"));

        ResourceResponse response = resourceService.create(request);
        assertNotNull(response);
        assertEquals("Projector", response.getName());
        assertEquals(new BigDecimal("150.00"), response.getPrice());
    }

    @Test
    @DisplayName("Should retrieve resource by id")
    void testGetById() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        ResourceResponse response = resourceService.getById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Projector", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when resource not found")
    void testGetById_NotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.getById(99L));
    }

    @Test
    @DisplayName("Should retrieve all resources")
    void testGetAll() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource));

        List<ResourceResponse> responses = resourceService.getAll();
        assertEquals(1, responses.size());
        assertEquals("Projector", responses.get(0).getName());
    }

    @Test
    @DisplayName("Should delete resource")
    void testDelete() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        resourceService.delete(1L);
        verify(resourceRepository).delete(resource);
    }
}
