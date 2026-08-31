package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.repository.ResourceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling CRUD operations and business logic for bookable {@link Resource} entities.
 */
@Service
@Transactional(readOnly = true)
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Creates a new resource entity from request details.
     *
     * @param request the resource creation request payload
     * @return the created resource response DTO
     */
    @Transactional
    public ResourceResponse create(ResourceRequest request) {

        Resource resource = new Resource();

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPrice(request.getPrice());

        Resource saved = resourceRepository.save(resource);
        log.info("Created new resource id={} name='{}'", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    /**
     * Retrieves all available and registered resources.
     *
     * @return list of all resource response DTOs
     */
    public List<ResourceResponse> getAll() {

        return resourceRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a resource by its unique identifier.
     *
     * @param id the resource ID
     * @return the resource response DTO
     * @throws ResourceNotFoundException if no resource exists with given ID
     */
    public ResourceResponse getById(Long id) {

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));

        return toResponse(resource);
    }

    /**
     * Updates an existing resource by its unique identifier.
     *
     * @param id      the resource ID to update
     * @param request the updated resource properties
     * @return the updated resource response DTO
     * @throws ResourceNotFoundException if no resource exists with given ID
     */
    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPrice(request.getPrice());

        Resource updated = resourceRepository.save(resource);
        log.info("Updated resource id={} name='{}'", updated.getId(), updated.getName());

        return toResponse(updated);
    }

    /**
     * Deletes a resource by its unique identifier.
     *
     * @param id the resource ID to delete
     * @throws ResourceNotFoundException if no resource exists with given ID
     */
    @Transactional
    public void delete(Long id) {

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + id));

        resourceRepository.delete(resource);
        log.info("Deleted resource id={}", id);
    }

    private ResourceResponse toResponse(Resource resource) {

        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.isAvailable(),
                resource.getPrice());
    }
}