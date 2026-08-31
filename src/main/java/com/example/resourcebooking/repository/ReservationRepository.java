package com.example.resourcebooking.repository;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.model.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Spring Data JPA repository for {@link Reservation} entities.
 */
public interface ReservationRepository
        extends JpaRepository<Reservation, Long>,
                JpaSpecificationExecutor<Reservation> {

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