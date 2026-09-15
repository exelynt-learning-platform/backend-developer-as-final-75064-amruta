package com.example.resourcebooking.dto;

import com.example.resourcebooking.constants.PaginationConstants;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.model.ReservationStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ReservationSearchCriteriaTest {

    @Test
    @DisplayName("Default constructor should set expected pagination and sort defaults")
    void testDefaultConstructor() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();

        assertEquals(PaginationConstants.DEFAULT_PAGE, criteria.getPage());
        assertEquals(PaginationConstants.DEFAULT_PAGE_SIZE, criteria.getSize());
        assertEquals(PaginationConstants.DEFAULT_SORT_DIRECTION, criteria.getDirection());
        assertNull(criteria.getSortBy());
        assertEquals(PaginationConstants.DEFAULT_SORT_FIELD, criteria.getValidSortBy());
        assertEquals(Sort.Direction.DESC, criteria.getSortDirection());
    }

    @Test
    @DisplayName("Parameterized constructor should initialize fields correctly")
    void testParameterizedConstructor() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria(
                ReservationStatus.CONFIRMED,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                1,
                25,
                "price",
                "asc"
        );

        assertEquals(ReservationStatus.CONFIRMED, criteria.getStatus());
        assertEquals(new BigDecimal("10.00"), criteria.getMinPrice());
        assertEquals(new BigDecimal("100.00"), criteria.getMaxPrice());
        assertEquals(1, criteria.getPage());
        assertEquals(25, criteria.getSize());
        assertEquals("price", criteria.getSortBy());
        assertEquals("asc", criteria.getDirection());
        assertEquals(Sort.Direction.ASC, criteria.getSortDirection());
    }

    @Test
    @DisplayName("validate() succeeds on valid criteria")
    void testValidate_Success() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setPage(0);
        criteria.setSize(20);
        criteria.setMinPrice(new BigDecimal("10.00"));
        criteria.setMaxPrice(new BigDecimal("50.00"));
        criteria.setSortBy("startTime");
        criteria.setDirection("asc");

        assertDoesNotThrow(criteria::validate);
    }

    @Test
    @DisplayName("validate() throws when page is negative")
    void testValidate_NegativePage() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setPage(-1);

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertEquals("Page cannot be negative", ex.getMessage());
    }

    @Test
    @DisplayName("validate() throws when size is below minimum")
    void testValidate_SizeBelowMinimum() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setSize(0);

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertTrue(ex.getMessage().contains("Size must be between"));
    }

    @Test
    @DisplayName("validate() throws when size exceeds maximum")
    void testValidate_SizeExceedsMaximum() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setSize(101);

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertTrue(ex.getMessage().contains("Size must be between"));
    }

    @Test
    @DisplayName("validate() throws when minPrice is negative")
    void testValidate_NegativeMinPrice() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setMinPrice(new BigDecimal("-1.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertEquals("Minimum price cannot be negative", ex.getMessage());
    }

    @Test
    @DisplayName("validate() throws when maxPrice is negative")
    void testValidate_NegativeMaxPrice() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setMaxPrice(new BigDecimal("-1.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertEquals("Maximum price cannot be negative", ex.getMessage());
    }

    @Test
    @DisplayName("validate() throws when minPrice is greater than maxPrice")
    void testValidate_MinPriceGreaterThanMaxPrice() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setMinPrice(new BigDecimal("100.00"));
        criteria.setMaxPrice(new BigDecimal("50.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertEquals("Minimum price cannot be greater than maximum price", ex.getMessage());
    }

    @Test
    @DisplayName("validate() throws when sort field is invalid")
    void testValidate_InvalidSortField() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setSortBy("unsupportedField");

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertTrue(ex.getMessage().contains("Invalid sort field"));
    }

    @Test
    @DisplayName("validate() throws when sort direction is invalid")
    void testValidate_InvalidSortDirection() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setDirection("sideways");

        BadRequestException ex = assertThrows(BadRequestException.class, criteria::validate);
        assertTrue(ex.getMessage().contains("Invalid sort direction"));
    }

    @Test
    @DisplayName("toPageable() constructs correct Pageable")
    void testToPageable() {
        ReservationSearchCriteria criteria = new ReservationSearchCriteria();
        criteria.setPage(2);
        criteria.setSize(25);
        criteria.setSortBy("price");
        criteria.setDirection("asc");

        Pageable pageable = criteria.toPageable();
        assertNotNull(pageable);
        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("price").isAscending());
    }
}
