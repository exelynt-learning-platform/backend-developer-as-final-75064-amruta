package com.example.resourcebooking.security;

import com.example.resourcebooking.repository.ReservationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Custom security evaluation service for method-level reservation access checks.
 */
@Component("reservationSecurity")
public class ReservationSecurityService {

    private final ReservationRepository reservationRepository;

    public ReservationSecurityService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Determines whether the currently authenticated principal is the owner of the given reservation.
     *
     * @param reservationId  the ID of the reservation to inspect
     * @param authentication the current Spring Security authentication object
     * @return true if the reservation exists and is owned by the current user, false otherwise
     */
    public boolean isOwner(Long reservationId, Authentication authentication) {
        if (reservationId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return reservationRepository.findById(reservationId)
                .map(reservation -> reservation.getUser() != null
                        && authentication.getName().equals(reservation.getUser().getUsername()))
                .orElse(false);
    }
}
