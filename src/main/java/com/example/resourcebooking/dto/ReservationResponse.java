package com.example.resourcebooking.dto;

import com.example.resourcebooking.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;

    private Long userId;

    private String username;

    private Long resourceId;

    private String resourceName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal price;

    private ReservationStatus status;

    private LocalDateTime createdAt;

    public ReservationResponse(
            Long id,
            Long userId,
            String username,
            Long resourceId,
            String resourceName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal price,
            ReservationStatus status,
            LocalDateTime createdAt) {

        this.id = id;
        this.userId = userId;
        this.username = username;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}