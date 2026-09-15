package com.example.resourcebooking.security;

import com.example.resourcebooking.model.Reservation;
import com.example.resourcebooking.repository.ReservationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Custom security evaluation service for method-level reservation access checks.
 * Caches loaded reservation instances in the request scope to eliminate redundant database queries.
 */
@Component("reservationSecurity")
public class ReservationSecurityService {

    public static final String CACHE_PREFIX = "RESERVATION_REQ_CACHE_";

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

        return findReservationCached(reservationId)
                .map(reservation -> reservation.getUser() != null
                        && authentication.getName().equals(reservation.getUser().getUsername()))
                .orElse(false);
    }

    /**
     * Retrieves a reservation from the request-scoped cache or loads it from the repository and caches it.
     *
     * @param reservationId reservation ID
     * @return optional containing reservation if found
     */
    public Optional<Reservation> findReservationCached(Long reservationId) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        String cacheKey = CACHE_PREFIX + reservationId;

        if (attributes != null) {
            Object cached = attributes.getAttribute(cacheKey, RequestAttributes.SCOPE_REQUEST);
            if (cached instanceof Reservation) {
                return Optional.of((Reservation) cached);
            }
        }

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isPresent() && attributes != null) {
            attributes.setAttribute(cacheKey, reservationOpt.get(), RequestAttributes.SCOPE_REQUEST);
        }

        return reservationOpt;
    }
}
