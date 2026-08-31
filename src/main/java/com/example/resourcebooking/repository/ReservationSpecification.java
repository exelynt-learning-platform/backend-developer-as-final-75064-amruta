package com.example.resourcebooking.repository;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Fluent Spring Data JPA specifications for filtering {@link Reservation} entities.
 */
public final class ReservationSpecification {

    private ReservationSpecification() {
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
