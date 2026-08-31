package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.repository.ResourceRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(
            ResourceRepository resourceRepository) {

        this.resourceRepository = resourceRepository;
    }

    public ResourceResponse create(
            ResourceRequest request) {

        Resource resource = new Resource();

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPrice(request.getPrice());

        return toResponse(
                resourceRepository.save(resource));
    }

    public List<ResourceResponse> getAll() {

        return resourceRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ResourceResponse getById(Long id) {

        Resource resource =
                resourceRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Resource not found with id: "
                                                + id));

        return toResponse(resource);
    }

    public ResourceResponse update(
            Long id,
            ResourceRequest request) {

        Resource resource =
                resourceRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Resource not found with id: "
                                                + id));

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPrice(request.getPrice());

        return toResponse(
                resourceRepository.save(resource));
    }

    public void delete(Long id) {

        Resource resource =
                resourceRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Resource not found with id: "
                                                + id));

        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(
            Resource resource) {

        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.isAvailable(),
                resource.getPrice());
    }
}