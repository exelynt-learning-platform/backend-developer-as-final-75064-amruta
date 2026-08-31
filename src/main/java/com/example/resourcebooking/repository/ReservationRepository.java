package com.example.resourcebooking.repository;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Reservation} entities with eager association fetching.
 */
public interface ReservationRepository
        extends JpaRepository<Reservation, Long>,
                JpaSpecificationExecutor<Reservation> {

    /**
     * Eagerly fetches user and resource associations for specifications to prevent LazyInitializationException under open-in-view=false.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "resource"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

    /**
     * Eagerly fetches user and resource associations for pagination without specifications.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "resource"})
    Page<Reservation> findAll(Pageable pageable);

    /**
     * Eagerly fetches user and resource associations for unpaged listings.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "resource"})
    List<Reservation> findAll();

    /**
     * Eagerly fetches user and resource associations by reservation ID.
     */
    @Override
    @EntityGraph(attributePaths = {"user", "resource"})
    Optional<Reservation> findById(Long id);

    /**
     * Checks if any reservation already exists for the given resource within an overlapping time range,
     * excluding a specified status (e.g., CANCELLED).
     *
     * @param resourceId     the ID of the resource
     * @param startTime      the requested start time
     * @param endTime        the requested end time
     * @param excludedStatus the reservation status to exclude (typically CANCELLED)
     * @return true if an overlapping reservation exists, false otherwise
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.resource.id = :resourceId " +
           "AND r.status <> :excludedStatus " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") ReservationStatus excludedStatus);

    /**
     * Checks if any reservation already exists for the given resource within an overlapping time range,
     * excluding a specified reservation ID and excluded status (used when updating an existing reservation).
     *
     * @param resourceId     the ID of the resource
     * @param reservationId  the ID of the reservation being updated to exclude from the check
     * @param startTime      the requested start time
     * @param endTime        the requested end time
     * @param excludedStatus the reservation status to exclude (typically CANCELLED)
     * @return true if an overlapping reservation exists, false otherwise
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.resource.id = :resourceId " +
           "AND r.id <> :reservationId " +
           "AND r.status <> :excludedStatus " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean existsOverlappingReservationExcludingId(
            @Param("resourceId") Long resourceId,
            @Param("reservationId") Long reservationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") ReservationStatus excludedStatus);
}