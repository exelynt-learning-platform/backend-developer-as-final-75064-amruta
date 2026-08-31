package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.service.ResourceService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceResponse> getAllResources() {
        return resourceService.getAll();
    }

    @PostMapping
    public ResourceResponse createResource(
            @RequestBody ResourceRequest request) {

        return resourceService.create(request);
    }

    @GetMapping("/{id}")
    public ResourceResponse getResourceById(
            @PathVariable Long id) {

        return resourceService.getById(id);
    }

    @PutMapping("/{id}")
    public ResourceResponse updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequest request) {

        return resourceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(
            @PathVariable Long id) {

        resourceService.delete(id);
    }
}