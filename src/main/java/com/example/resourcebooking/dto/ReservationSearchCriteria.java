package com.example.resourcebooking.dto;

import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.ReservationStatus;

import java.math.BigDecimal;

/**
 * Data Transfer Object encapsulating search, filter, and pagination parameters for reservations.
 * Validates pagination boundaries and sort directions early upon construction and mutation.
 */
public class ReservationSearchCriteria {

    private ReservationStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = 0;
    private int size = 10;
    private String sortBy;
    private String direction = "desc";

    public ReservationSearchCriteria() {
    }

    public ReservationSearchCriteria(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {
        this.status = status;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        setDirection(direction);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            this.direction = "desc";
            return;
        }

        String normalized = direction.trim().toLowerCase();
        if (!"asc".equals(normalized) && !"desc".equals(normalized)) {
            throw new BadRequestException("Invalid sort direction '" + direction + "'. Allowed values: 'asc', 'desc'");
        }

        this.direction = normalized;
    }
}
