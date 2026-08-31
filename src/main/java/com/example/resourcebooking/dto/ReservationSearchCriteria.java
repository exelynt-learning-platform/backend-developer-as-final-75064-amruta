package com.example.resourcebooking.dto;

import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.ReservationStatus;

import java.math.BigDecimal;

/**
 * Data Transfer Object encapsulating search, filter, and pagination parameters for reservations.
 * Validates pagination boundaries, price ranges, and sort directions early upon construction and mutation.
 */
public class ReservationSearchCriteria {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

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
        setPage(page);
        setSize(size);
        setPriceRange(minPrice, maxPrice);
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
        setPriceRange(minPrice, this.maxPrice);
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        setPriceRange(this.minPrice, maxPrice);
    }

    public void setPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum price cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum price cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        if (page < 0) {
            throw new BadRequestException("Page cannot be negative");
        }
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Size must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE);
        }
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
