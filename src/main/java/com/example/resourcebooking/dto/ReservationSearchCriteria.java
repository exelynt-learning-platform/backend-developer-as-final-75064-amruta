package com.example.resourcebooking.dto;

import com.example.resourcebooking.constants.PaginationConstants;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.ReservationStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

/**
 * Data Transfer Object encapsulating search, filter, and pagination parameters for reservations.
 * Centralizes validation of pagination bounds, price ranges, and sorting parameters.
 */
public class ReservationSearchCriteria {

    private ReservationStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = PaginationConstants.DEFAULT_PAGE;
    private int size = PaginationConstants.DEFAULT_PAGE_SIZE;
    private String sortBy;
    private String direction = PaginationConstants.DEFAULT_SORT_DIRECTION;

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
        this.direction = (direction != null && !direction.isBlank()) ? direction : PaginationConstants.DEFAULT_SORT_DIRECTION;
    }

    /**
     * Validates all search criteria fields including pagination bounds, price ranges, and sorting options.
     * Throws BadRequestException if any parameter is invalid.
     */
    public void validate() {
        validatePagination();
        validatePriceRange();
        validateSorting();
    }

    private void validatePagination() {
        if (page < 0) {
            throw new BadRequestException("Page cannot be negative");
        }
        if (size < PaginationConstants.MIN_PAGE_SIZE || size > PaginationConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Size must be between " + PaginationConstants.MIN_PAGE_SIZE + " and " + PaginationConstants.MAX_PAGE_SIZE);
        }
    }

    private void validatePriceRange() {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum price cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum price cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
    }

    private void validateSorting() {
        if (sortBy != null && !sortBy.isBlank() && !PaginationConstants.ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field. Allowed: " + PaginationConstants.ALLOWED_SORT_FIELDS);
        }
        if (direction != null && !direction.isBlank()
                && !"asc".equalsIgnoreCase(direction)
                && !"desc".equalsIgnoreCase(direction)) {
            throw new BadRequestException(
                    "Invalid sort direction '" + direction + "'. Allowed values: 'asc', 'desc'");
        }
    }

    /**
     * Resolves the sort direction, defaulting to DESC if not explicitly set to ASC.
     * Does not throw from getter to avoid unexpected binding/reflection errors.
     *
     * @return Sort.Direction (ASC or DESC)
     */
    public Sort.Direction getSortDirection() {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    /**
     * Returns the valid sort-by field name, defaulting to 'createdAt' if not specified.
     *
     * @return valid sort field name
     */
    public String getValidSortBy() {
        if (sortBy == null || sortBy.isBlank()) {
            return PaginationConstants.DEFAULT_SORT_FIELD;
        }
        return sortBy;
    }

    /**
     * Creates a Spring Data Pageable instance based on validated pagination and sorting parameters.
     *
     * @return Pageable object
     */
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(getSortDirection(), getValidSortBy()));
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
        this.direction = direction;
    }
}
