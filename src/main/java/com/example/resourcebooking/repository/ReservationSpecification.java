package com.example.resourcebooking.repository;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;
import com.example.resourcebooking.model.Role;
import com.example.resourcebooking.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Fluent Spring Data JPA specifications and composite builder for filtering {@link Reservation} entities.
 */
public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    /**
     * Builds a composite specification combining role-based ownership, status filter, and price boundaries.
     *
     * @param user     authenticated user (if regular USER, limits search to user's own reservations)
     * @param status   optional reservation status filter
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @return combined JPA Specification for Reservation entities
     */
    public static Specification<Reservation> buildSpecification(
            User user,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return Specification
                .where(user != null && user.getRole() == Role.USER ? hasUser(user.getId()) : null)
                .and(hasStatus(status))
                .and(priceGreaterThanOrEqualTo(minPrice))
                .and(priceLessThanOrEqualTo(maxPrice));
    }

    /**
     * Filters reservations belonging to a specific user ID.
     */
    public static Specification<Reservation> hasUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                userId == null ? null : criteriaBuilder.equal(root.join("user").get("id"), userId);
    }

    /**
     * Filters reservations by booking status.
     */
    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Filters reservations with price greater than or equal to minPrice.
     */
    public static Specification<Reservation> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                minPrice == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    /**
     * Filters reservations with price less than or equal to maxPrice.
     */
    public static Specification<Reservation> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                maxPrice == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
