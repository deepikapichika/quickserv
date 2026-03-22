package com.quickserv.quickserv.dto.booking;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for creating a new booking
 * Validates all required fields for booking creation
 */
public class BookingCreateRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Booking date and time is required")
    private LocalDateTime bookingDateTime;

    @Size(max = 1000, message = "Customer notes cannot exceed 1000 characters")
    private String customerNotes;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "^(CARD|UPI|WALLET|CASH)$", message = "Payment method must be CARD, UPI, WALLET, or CASH")
    private String paymentMethod;

    private List<Long> addonIds; // IDs of add-ons selected

    @Pattern(regexp = "^[A-Z0-9]*$", message = "Invalid coupon code format")
    @Size(max = 20, message = "Coupon code cannot exceed 20 characters")
    private String couponCode;

    private BigDecimal serviceCharge = BigDecimal.ZERO;

    // Constructors
    public BookingCreateRequest() {
    }

    public BookingCreateRequest(Long serviceId, LocalDateTime bookingDateTime, String paymentMethod) {
        this.serviceId = serviceId;
        this.bookingDateTime = bookingDateTime;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<Long> getAddonIds() {
        return addonIds;
    }

    public void setAddonIds(List<Long> addonIds) {
        this.addonIds = addonIds;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(BigDecimal serviceCharge) {
        this.serviceCharge = serviceCharge;
    }
}

