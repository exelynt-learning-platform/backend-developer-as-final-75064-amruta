package com.example.resourcebooking.constants;

import java.util.List;

/**
 * Constants defining pagination bounds, default sorting, and allowed sort fields across the application.
 */
public final class PaginationConstants {

    private PaginationConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "desc";
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    public static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id",
            "price",
            "startTime",
            "endTime",
            "createdAt",
            "status"
    );
}
