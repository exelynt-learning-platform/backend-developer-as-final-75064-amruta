package com.example.resourcebooking.controller;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.service.ReservationService;

import javax.validation.Valid;

/**
 * REST controller for managing reservations including creating, searching, updating, and cancelling bookings.
 */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Creates a new reservation for the currently authenticated user.
     *
     * @param request        reservation details
     * @param authentication current user authentication
     * @return created reservation
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request, authentication));
    }

    /**
     * Retrieves a paginated and filtered list of reservations.
     *
     * @param authentication current user authentication
     * @param status         optional reservation status filter
     * @param minPrice       optional minimum price filter
     * @param maxPrice       optional maximum price filter
     * @param page           page index (default: 0)
     * @param size           page size (default: 10)
     * @param sortBy         field to sort by
     * @param direction      sort direction ("asc" or "desc")
     * @return page of reservations
     */
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getReservations(
            Authentication authentication,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                reservationService.getReservations(
                        authentication,
                        status,
                        minPrice,
                        maxPrice,
                        page,
                        size,
                        sortBy,
                        direction));
    }

    /**
     * Retrieves a reservation by its ID.
     *
     * @param id             reservation ID
     * @param authentication current user authentication
     * @return reservation details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.getById(id, authentication));
    }

    /**
     * Updates an existing reservation.
     *
     * @param id             reservation ID
     * @param request        updated reservation details
     * @param authentication current user authentication
     * @return updated reservation
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.update(id, request, authentication));
    }

    /**
     * Deletes / cancels a reservation by its ID.
     *
     * @param id             reservation ID
     * @param authentication current user authentication
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {

        reservationService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}