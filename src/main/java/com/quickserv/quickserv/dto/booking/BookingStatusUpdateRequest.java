package com.quickserv.quickserv.dto.booking;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for updating booking status by provider
 */
public class BookingStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED|REJECTED|RESCHEDULED)$",
             message = "Invalid status. Must be one of: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED, RESCHEDULED")
    private String status;

    @Size(max = 1000, message = "Provider notes cannot exceed 1000 characters")
    private String providerNotes;

    private BigDecimal actualAmount; // For final billing adjustments

    // Constructors
    public BookingStatusUpdateRequest() {
    }

    public BookingStatusUpdateRequest(String status) {
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderNotes() {
        return providerNotes;
    }

    public void setProviderNotes(String providerNotes) {
        this.providerNotes = providerNotes;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }
}

