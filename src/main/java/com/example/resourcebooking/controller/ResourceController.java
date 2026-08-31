package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.service.ResourceService;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing system resources (e.g. rooms, equipment, vehicles).
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Retrieves all registered resources in the system.
     *
     * @return list of resources
     */
    @GetMapping
    public List<ResourceResponse> getAllResources() {

        return resourceService.getAll();
    }

    /**
     * Creates a new resource. Requires ADMIN role.
     *
     * @param request resource creation details
     * @return created resource
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceService.create(request));
    }

    /**
     * Retrieves a resource by its ID.
     *
     * @param id resource ID
     * @return resource details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                resourceService.getById(id));
    }

    /**
     * Updates an existing resource. Requires ADMIN role.
     *
     * @param id      resource ID
     * @param request updated resource details
     * @return updated resource
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {

        return ResponseEntity.ok(
                resourceService.update(id, request));
    }

    /**
     * Deletes a resource. Requires ADMIN role.
     *
     * @param id resource ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}