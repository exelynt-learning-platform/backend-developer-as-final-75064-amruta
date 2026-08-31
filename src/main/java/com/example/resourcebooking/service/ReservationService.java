package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationNotFoundException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.*;
import com.example.resourcebooking.repository.*;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

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

    public ReservationResponse create(
            ReservationRequest request,
            Authentication authentication) {

        validateTimes(
                request.getStartTime(),
                request.getEndTime());

        User user = getAuthenticatedUser(authentication);

        Resource resource =
                resourceRepository.findById(request.getResourceId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Resource not found with id: "
                                                + request.getResourceId()));

        if (!resource.isAvailable()) {
            throw new BadRequestException(
                    "Resource is not available");
        }

        Reservation reservation = new Reservation();

        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());

        // Price is taken from the resource.
        reservation.setPrice(resource.getPrice());

        reservation.setStatus(ReservationStatus.PENDING);

        return toResponse(
                reservationRepository.save(reservation));
    }

    public Page<ReservationResponse> getReservations(
            Authentication authentication,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new BadRequestException(
                    "Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "Size must be between 1 and 100");
        }

        if (minPrice != null &&
                minPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BadRequestException(
                    "Minimum price cannot be negative");
        }

        if (maxPrice != null &&
                maxPrice.compareTo(BigDecimal.ZERO) < 0) {

            throw new BadRequestException(
                    "Maximum price cannot be negative");
        }

        if (minPrice != null &&
                maxPrice != null &&
                minPrice.compareTo(maxPrice) > 0) {

            throw new BadRequestException(
                    "Minimum price cannot be greater than maximum price");
        }

        User user = getAuthenticatedUser(authentication);

        Sort sort = createSort(sortBy, direction);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        sort);

        Specification<Reservation> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> predicates =
                            new ArrayList<>();

                    if (user.getRole() == Role.USER) {

                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("user").get("id"),
                                        user.getId()
                                )
                        );
                    }

                    if (status != null) {

                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("status"),
                                        status
                                )
                        );
                    }

                    if (minPrice != null) {

                        predicates.add(
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.get("price"),
                                        minPrice
                                )
                        );
                    }

                    if (maxPrice != null) {

                        predicates.add(
                                criteriaBuilder.lessThanOrEqualTo(
                                        root.get("price"),
                                        maxPrice
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            predicates.toArray(
                                    new Predicate[0]));
                };

        return reservationRepository
                .findAll(specification, pageable)
                .map(this::toResponse);
    }

    public ReservationResponse getById(
            Long id,
            Authentication authentication) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        "Reservation not found with id: "
                                                + id));

        User user =
                getAuthenticatedUser(authentication);

        checkOwnership(reservation, user);

        return toResponse(reservation);
    }

    public ReservationResponse update(
            Long id,
            ReservationRequest request) {

        validateTimes(
                request.getStartTime(),
                request.getEndTime());

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        "Reservation not found with id: "
                                                + id));

        Resource resource =
                resourceRepository.findById(request.getResourceId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Resource not found with id: "
                                                + request.getResourceId()));

        if (!resource.isAvailable()) {
            throw new BadRequestException(
                    "Resource is not available");
        }

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(resource.getPrice());

        return toResponse(
                reservationRepository.save(reservation));
    }

    public void delete(Long id) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ReservationNotFoundException(
                                        "Reservation not found with id: "
                                                + id));

        reservationRepository.delete(reservation);
    }

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication required");
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated user not found"));
    }

    private void checkOwnership(
            Reservation reservation,
            User user) {

        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (!reservation.getUser()
                .getId()
                .equals(user.getId())) {

            throw new AccessDeniedException(
                    "You can access only your own reservations");
        }
    }

    private void validateTimes(
            LocalDateTime start,
            LocalDateTime end) {

        if (!end.isAfter(start)) {
            throw new BadRequestException(
                    "End time must be after start time");
        }
    }

    private Sort createSort(
            String sortBy,
            String direction) {

        if (sortBy == null ||
                sortBy.isBlank()) {

            return Sort.by(
                    Sort.Direction.DESC,
                    "createdAt");
        }

        List<String> allowedFields =
                List.of(
                        "id",
                        "price",
                        "startTime",
                        "endTime",
                        "createdAt",
                        "status"
                );

        if (!allowedFields.contains(sortBy)) {

            throw new BadRequestException(
                    "Invalid sort field. Allowed: "
                            + allowedFields);
        }

        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        return Sort.by(
                sortDirection,
                sortBy);
    }

    private ReservationResponse toResponse(
            Reservation reservation) {

        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt());
    }
}
