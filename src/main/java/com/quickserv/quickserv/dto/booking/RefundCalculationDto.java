package com.quickserv.quickserv.dto.booking;

import java.math.BigDecimal;

/**
 * DTO for refund calculation details
 */
public class RefundCalculationDto {

    private Long bookingId;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private BigDecimal refundPercentage;
    private String refundPolicy;
    private String refundReason;
    private boolean isProviderNoShow;
    private BigDecimal providerNoShowCredit;
    private String hoursUntilService;

    // Constructors
    public RefundCalculationDto() {
    }

    public RefundCalculationDto(Long bookingId, BigDecimal totalAmount,
                               BigDecimal refundAmount, String refundReason) {
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        calculateRefundPercentage();
    }

    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
        calculateRefundPercentage();
    }

    public BigDecimal getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(BigDecimal refundPercentage) { this.refundPercentage = refundPercentage; }

    public String getRefundPolicy() { return refundPolicy; }
    public void setRefundPolicy(String refundPolicy) { this.refundPolicy = refundPolicy; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public boolean isProviderNoShow() { return isProviderNoShow; }
    public void setProviderNoShow(boolean providerNoShow) { isProviderNoShow = providerNoShow; }

    public BigDecimal getProviderNoShowCredit() { return providerNoShowCredit; }
    public void setProviderNoShowCredit(BigDecimal providerNoShowCredit) { this.providerNoShowCredit = providerNoShowCredit; }

    public String getHoursUntilService() { return hoursUntilService; }
    public void setHoursUntilService(String hoursUntilService) { this.hoursUntilService = hoursUntilService; }

    /**
     * Calculate refund percentage
     */
    private void calculateRefundPercentage() {
        if (totalAmount != null && refundAmount != null &&
            totalAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            this.refundPercentage = refundAmount.multiply(java.math.BigDecimal.valueOf(100))
                .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Get complete refund summary
     */
    public String getRefundSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking ID: ").append(bookingId).append("\n");
        sb.append("Total Amount: ₹").append(totalAmount).append("\n");
        sb.append("Refund Amount: ₹").append(refundAmount).append("\n");
        sb.append("Refund Percentage: ").append(refundPercentage).append("%\n");
        sb.append("Reason: ").append(refundReason).append("\n");
        if (isProviderNoShow) {
            sb.append("Provider No-Show: YES\n");
            sb.append("Additional Credit: ₹").append(providerNoShowCredit).append("\n");
        }
        return sb.toString();
    }
}

