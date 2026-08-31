package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationNotFoundException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.model.Resource;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        public ReservationResponse create(
                        ReservationRequest request,
                        Authentication authentication) {

                validateTimes(
                                request.getStartTime(),
                                request.getEndTime());

                User user = getAuthenticatedUser(authentication);

                Resource resource = findResource(
                                request.getResourceId());

                validateResourceAvailability(resource);

                Reservation reservation = new Reservation();

                reservation.setUser(user);
                reservation.setResource(resource);
                reservation.setStartTime(request.getStartTime());
                reservation.setEndTime(request.getEndTime());
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

                validatePagination(page, size);
                validatePriceRange(minPrice, maxPrice);

                User user = getAuthenticatedUser(authentication);
                Pageable pageable = createPageable(
                                page,
                                size,
                                sortBy,
                                direction);

                Specification<Reservation> specification = createReservationSpecification(
                                user,
                                status,
                                minPrice,
                                maxPrice);

                return findReservations(
                                specification,
                                pageable);
        }

        public ReservationResponse getById(
                        Long id,
                        Authentication authentication) {

                Reservation reservation = findReservation(id);
                User user = getAuthenticatedUser(authentication);

                checkOwnership(reservation, user);

                return toResponse(reservation);
        }

        public ReservationResponse update(
                        Long id,
                        ReservationRequest request,
                        Authentication authentication) {

                validateTimes(
                                request.getStartTime(),
                                request.getEndTime());

                Reservation reservation = findReservation(id);
                User user = getAuthenticatedUser(authentication);

                checkOwnership(reservation, user);

                Resource resource = findResource(
                                request.getResourceId());

                validateResourceAvailability(resource);

                reservation.setResource(resource);
                reservation.setStartTime(request.getStartTime());
                reservation.setEndTime(request.getEndTime());
                reservation.setPrice(resource.getPrice());

                return toResponse(
                                reservationRepository.save(reservation));
        }

        public void delete(
                        Long id,
                        Authentication authentication) {

                Reservation reservation = findReservation(id);
                User user = getAuthenticatedUser(authentication);

                checkOwnership(reservation, user);

                reservationRepository.delete(reservation);
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

        private Specification<Reservation> createReservationSpecification(
                        User user,
                        ReservationStatus status,
                        BigDecimal minPrice,
                        BigDecimal maxPrice) {

                return (root, query, criteriaBuilder) -> {

                        List<Predicate> predicates = new ArrayList<>();

                        addOwnershipPredicate(
                                        predicates,
                                        root,
                                        criteriaBuilder,
                                        user);

                        addStatusPredicate(
                                        predicates,
                                        root,
                                        criteriaBuilder,
                                        status);

                        addMinPricePredicate(
                                        predicates,
                                        root,
                                        criteriaBuilder,
                                        minPrice);

                        addMaxPricePredicate(
                                        predicates,
                                        root,
                                        criteriaBuilder,
                                        maxPrice);

                        return criteriaBuilder.and(
                                        predicates.toArray(new Predicate[0]));
                };
        }

        private void addOwnershipPredicate(
                        List<Predicate> predicates,
                        Root<Reservation> root,
                        CriteriaBuilder criteriaBuilder,
                        User user) {

                if (user.getRole() == Role.USER) {

                        predicates.add(
                                        criteriaBuilder.equal(
                                                        root.get("user").get("id"),
                                                        user.getId()));
                }
        }

        private void addStatusPredicate(
                        List<Predicate> predicates,
                        Root<Reservation> root,
                        CriteriaBuilder criteriaBuilder,
                        ReservationStatus status) {

                if (status != null) {

                        predicates.add(
                                        criteriaBuilder.equal(
                                                        root.get("status"),
                                                        status));
                }
        }

        private void addMinPricePredicate(
                        List<Predicate> predicates,
                        Root<Reservation> root,
                        CriteriaBuilder criteriaBuilder,
                        BigDecimal minPrice) {

                if (minPrice != null) {

                        predicates.add(
                                        criteriaBuilder.greaterThanOrEqualTo(
                                                        root.get("price"),
                                                        minPrice));
                }
        }

        private void addMaxPricePredicate(
                        List<Predicate> predicates,
                        Root<Reservation> root,
                        CriteriaBuilder criteriaBuilder,
                        BigDecimal maxPrice) {

                if (maxPrice != null) {

                        predicates.add(
                                        criteriaBuilder.lessThanOrEqualTo(
                                                        root.get("price"),
                                                        maxPrice));
                }
        }

        private Reservation findReservation(Long id) {

                return reservationRepository
                                .findById(id)
                                .orElseThrow(() -> new ReservationNotFoundException(
                                                "Reservation not found with id: "
                                                                + id));
        }

        private Resource findResource(Long resourceId) {

                return resourceRepository
                                .findById(resourceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Resource not found with id: "
                                                                + resourceId));
        }

        private void validateResourceAvailability(
                        Resource resource) {

                if (!resource.isAvailable()) {

                        throw new BadRequestException(
                                        "Resource is not available");
                }
        }

        private void validatePagination(
                        int page,
                        int size) {

                if (page < 0) {

                        throw new BadRequestException(
                                        "Page cannot be negative");
                }

                if (size < MIN_PAGE_SIZE ||
                                size > MAX_PAGE_SIZE) {

                        throw new BadRequestException(
                                        "Size must be between "
                                                        + MIN_PAGE_SIZE
                                                        + " and "
                                                        + MAX_PAGE_SIZE);
                }
        }

        private void validatePriceRange(
                        BigDecimal minPrice,
                        BigDecimal maxPrice) {

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
                                .orElseThrow(() -> new AccessDeniedException(
                                                "Authenticated user not found"));
        }

        private void checkOwnership(
                        Reservation reservation,
                        User user) {

                if (user.getRole() == Role.ADMIN) {
                        return;
                }

                if (reservation.getUser() == null ||
                                reservation.getUser().getId() == null ||
                                !reservation.getUser()
                                                .getId()
                                                .equals(user.getId())) {

                        throw new AccessDeniedException(
                                        "You can access only your own reservations");
                }
        }

        private void validateTimes(
                        LocalDateTime start,
                        LocalDateTime end) {

                if (start == null || end == null) {

                        throw new BadRequestException(
                                        "Start time and end time are required");
                }

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

                validateSortField(sortBy);

                Sort.Direction sortDirection = getSortDirection(direction);

                return Sort.by(
                                sortDirection,
                                sortBy);
        }

        private void validateSortField(
                        String sortBy) {

                if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {

                        throw new BadRequestException(
                                        "Invalid sort field. Allowed: "
                                                        + ALLOWED_SORT_FIELDS);
                }
        }

        private Sort.Direction getSortDirection(
                        String direction) {

                return "desc".equalsIgnoreCase(direction)
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
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
