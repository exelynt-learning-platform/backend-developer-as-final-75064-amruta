package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.dto.ReservationSearchCriteria;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationNotFoundException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ReservationSpecification;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class responsible for managing reservation lifecycles, including creation,
 * retrieval, overlapping time slot validation, updates, cancellation, and authorization checks.
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "price",
            "startTime",
            "endTime",
            "createdAt",
            "status");

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new reservation for an authenticated user after validating times, resource availability,
     * role-based status assignment, and ensuring there are no overlapping bookings for the selected time window.
     *
     * @param request        the reservation request payload containing resource ID and time interval
     * @param authentication the Spring Security authentication object of the current user
     * @return the created reservation response DTO
     * @throws BadRequestException if times are invalid, resource unavailable, or time slot is already booked
     */
    @Transactional
    public ReservationResponse create(
            ReservationRequest request,
            Authentication authentication) {

        validateTimes(request.getStartTime(), request.getEndTime());

        User user = getAuthenticatedUser(authentication);
        Resource resource = findResource(request.getResourceId());

        validateResourceAvailability(resource);
        validateNoOverlappingReservation(resource.getId(), null, request.getStartTime(), request.getEndTime());

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(resource.getPrice());

        // Role-based status enforcement:
        // Regular users always create PENDING reservations.
        // Admins can specify status or default to CONFIRMED.
        if (user.getRole() == Role.ADMIN) {
            reservation.setStatus(request.getStatus() != null ? request.getStatus() : ReservationStatus.CONFIRMED);
        } else {
            reservation.setStatus(ReservationStatus.PENDING);
        }

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Created reservation id={} for user='{}' [role={}] on resource='{}' with status={}",
                savedReservation.getId(), user.getUsername(), user.getRole(), resource.getName(),
                savedReservation.getStatus());

        return toResponse(savedReservation);
    }

    /**
     * Retrieves a paginated list of reservations filtered by status, price range, and user permissions
     * using fluent specifications. Non-admin users are restricted to viewing only their own reservations.
     *
     * @param authentication current user authentication
     * @param criteria       search criteria containing status, price range, pagination, and sorting
     * @return page of matching reservation response DTOs
     */
    public Page<ReservationResponse> getReservations(
            Authentication authentication,
            ReservationSearchCriteria criteria) {

        if (criteria == null) {
            criteria = new ReservationSearchCriteria();
        }

        validatePagination(criteria.getPage(), criteria.getSize());
        validatePriceRange(criteria.getMinPrice(), criteria.getMaxPrice());

        User user = getAuthenticatedUser(authentication);
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection());

        Specification<Reservation> specification = Specification
                .where(user.getRole() == Role.USER ? ReservationSpecification.hasUser(user.getId()) : null)
                .and(ReservationSpecification.hasStatus(criteria.getStatus()))
                .and(ReservationSpecification.priceGreaterThanOrEqualTo(criteria.getMinPrice()))
                .and(ReservationSpecification.priceLessThanOrEqualTo(criteria.getMaxPrice()));

        return findReservations(specification, pageable);
    }

    /**
     * Retrieves a single reservation by ID after verifying the caller's access permissions.
     *
     * @param id             the reservation ID
     * @param authentication current user authentication
     * @return the reservation response DTO
     * @throws ReservationNotFoundException if no reservation exists with the given ID
     * @throws AccessDeniedException        if the user is not an admin and does not own the reservation
     */
    public ReservationResponse getById(
            Long id,
            Authentication authentication) {

        Reservation reservation = findReservation(id);
        User user = getAuthenticatedUser(authentication);

        checkOwnership(reservation, user);

        return toResponse(reservation);
    }

    /**
     * Updates an existing reservation's resource, time window, and status.
     * When updated by a regular user, the status is strictly reset to PENDING for re-approval.
     *
     * @param id             the reservation ID to update
     * @param request        the updated reservation details
     * @param authentication current user authentication
     * @return the updated reservation response DTO
     */
    @Transactional
    public ReservationResponse update(
            Long id,
            ReservationRequest request,
            Authentication authentication) {

        validateTimes(request.getStartTime(), request.getEndTime());

        Reservation reservation = findReservation(id);
        User user = getAuthenticatedUser(authentication);

        checkOwnership(reservation, user);

        Resource resource = findResource(request.getResourceId());
        validateResourceAvailability(resource);
        validateNoOverlappingReservation(resource.getId(), reservation.getId(), request.getStartTime(), request.getEndTime());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(resource.getPrice());

        // Status update logic:
        // If updated by ADMIN, apply requested status if provided.
        // If updated by regular USER, reset status to PENDING so changes require re-approval.
        if (user.getRole() == Role.ADMIN) {
            if (request.getStatus() != null) {
                reservation.setStatus(request.getStatus());
            }
        } else {
            reservation.setStatus(ReservationStatus.PENDING);
        }

        Reservation updatedReservation = reservationRepository.save(reservation);
        log.info("Updated reservation id={} by user='{}' [role={}], status={}",
                id, user.getUsername(), user.getRole(), updatedReservation.getStatus());

        return toResponse(updatedReservation);
    }

    /**
     * Deletes a reservation by ID after confirming ownership and access permissions.
     *
     * @param id             the reservation ID
     * @param authentication current user authentication
     */
    @Transactional
    public void delete(
            Long id,
            Authentication authentication) {

        Reservation reservation = findReservation(id);
        User user = getAuthenticatedUser(authentication);

        checkOwnership(reservation, user);

        reservationRepository.delete(reservation);
        log.info("Deleted reservation id={} by user='{}'", id, user.getUsername());
    }

    /**
     * Validates that the requested resource is not already reserved during the specified time slot.
     *
     * @param resourceId           the resource ID
     * @param excludeReservationId the reservation ID to exclude from conflict check (if updating)
     * @param startTime            start of desired slot
     * @param endTime              end of desired slot
     */
    private void validateNoOverlappingReservation(
            Long resourceId,
            Long excludeReservationId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        boolean hasOverlap;
        if (excludeReservationId == null) {
            hasOverlap = reservationRepository.existsOverlappingReservation(
                    resourceId,
                    startTime,
                    endTime,
                    ReservationStatus.CANCELLED);
        } else {
            hasOverlap = reservationRepository.existsOverlappingReservationExcludingId(
                    resourceId,
                    excludeReservationId,
                    startTime,
                    endTime,
                    ReservationStatus.CANCELLED);
        }

        if (hasOverlap) {
            throw new BadRequestException("Resource is already booked for the selected time slot");
        }
    }

    private Page<ReservationResponse> findReservations(
            Specification<Reservation> specification,
            Pageable pageable) {

        return reservationRepository
                .findAll(specification, pageable)
                .map(this::toResponse);
    }

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction) {

        return PageRequest.of(
                page,
                size,
                createSort(sortBy, direction));
    }

    private Reservation findReservation(Long id) {

        return reservationRepository
                .findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found with id: " + id));
    }

    private Resource findResource(Long resourceId) {

        return resourceRepository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + resourceId));
    }

    private void validateResourceAvailability(Resource resource) {

        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource is not available");
        }
    }

    private void validatePagination(int page, int size) {

        if (page < 0) {
            throw new BadRequestException("Page cannot be negative");
        }

        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Size must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE);
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum price cannot be negative");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum price cannot be negative");
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BadCredentialsException("Authentication required");
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
    }

    private void checkOwnership(Reservation reservation, User user) {

        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (reservation.getUser() == null ||
                reservation.getUser().getId() == null ||
                !reservation.getUser().getId().equals(user.getId())) {

            throw new AccessDeniedException("You can access only your own reservations");
        }
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {

        if (start == null || end == null) {
            throw new BadRequestException("Start time and end time are required");
        }

        if (!end.isAfter(start)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private Sort createSort(String sortBy, String direction) {

        Sort.Direction sortDirection = getSortDirection(direction);

        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(sortDirection, "createdAt");
        }

        validateSortField(sortBy);

        return Sort.by(sortDirection, sortBy);
    }

    private void validateSortField(String sortBy) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field. Allowed: " + ALLOWED_SORT_FIELDS);
        }
    }

    private Sort.Direction getSortDirection(String direction) {

        if (direction == null || direction.isBlank() || "desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }

        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }

        throw new BadRequestException("Invalid sort direction '" + direction + "'. Allowed values: 'asc', 'desc'");
    }

    private ReservationResponse toResponse(Reservation reservation) {

        return ReservationResponse.builder()
                .id(reservation.getId())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
