package com.quickserv.quickserv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_provider_id", columnList = "provider_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_booking_date", columnList = "booking_date_time")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceListing service;

    @Column(nullable = false)
    private LocalDateTime bookingDateTime;

    @Column(length = 1000)
    private String customerNotes;

    @Column(length = 1000)
    private String providerNotes;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    // Pricing breakdown fields
    private BigDecimal basePrice;  // Service base price
    private BigDecimal travelCharge = BigDecimal.ZERO;  // Distance-based charge
    private BigDecimal addonCharges = BigDecimal.ZERO;  // Add-on services charge
    private BigDecimal couponDiscount = BigDecimal.ZERO;  // Coupon applied discount
    private BigDecimal gstAmount;  // 18% GST
    private BigDecimal totalAmount;  // Final total

    // Booking details
    private BigDecimal distanceKm = BigDecimal.ZERO;  // Distance from customer to provider

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "addon_ids", length = 500)
    private String addonIds;  // Comma-separated addon IDs

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Booking lifecycle timestamps
    private LocalDateTime confirmedAt;  // When customer confirmed
    private LocalDateTime assignedAt;  // When provider was assigned
    private LocalDateTime startedAt;  // When service started
    private LocalDateTime completedAt;  // When service completed
    private LocalDateTime cancelledAt;  // When booking was cancelled
    private LocalDateTime rescheduledAt;  // When booking was rescheduled

    // Cancellation and refund tracking
    @Column(length = 1000)
    private String cancellationNotes;  // Reason for cancellation
    private BigDecimal refundAmount;  // Amount refunded
    private Boolean isRefunded = false;  // Refund status
    @Column(length = 500)
    private String refundReason;  // Reason for refund

    // Provider assignment scoring
    private BigDecimal assignedProviderScore;  // Score used for assignment

    // System tracking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;  // Optimistic locking

    // ==================== Enums ====================

    public enum BookingStatus {
        PENDING,        // Initial state
        CONFIRMED,      // Customer confirmed, awaiting assignment
        ASSIGNED,       // Provider assigned
        IN_PROGRESS,    // Service in progress
        COMPLETED,      // Service completed
        CANCELLED,      // Booking cancelled
        REJECTED,       // Rejected by provider
        RESCHEDULED     // Rescheduled to new date/time
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Business Logic Methods ====================

    /**
     * Calculate refund based on cancellation policy:
     * >24 hours before: Full refund
     * 12-24 hours before: 50% refund
     * <12 hours before: No refund
     */
    public BigDecimal calculateRefundAmount() {
        if (LocalDateTime.now().isBefore(bookingDateTime.minusHours(24))) {
            return totalAmount;  // Full refund
        } else if (LocalDateTime.now().isBefore(bookingDateTime.minusHours(12))) {
            return totalAmount.multiply(BigDecimal.valueOf(0.5));  // 50% refund
        }
        return BigDecimal.ZERO;  // No refund
    }

    /**
     * Apply provider no-show policy: Full refund + ₹100 credit
     */
    public BigDecimal getRefundWithProviderNoShowCredit() {
        return totalAmount.add(BigDecimal.valueOf(100));
    }

    /**
     * Check if booking can be rescheduled
     */
    public boolean canBeRescheduled() {
        return status != BookingStatus.COMPLETED &&
               status != BookingStatus.CANCELLED;
    }

    /**
     * Check if booking can be cancelled based on current status
     */
    public boolean canBeCancelled() {
        return status == BookingStatus.PENDING ||
               status == BookingStatus.CONFIRMED ||
               status == BookingStatus.ASSIGNED;
    }

    // Constructors
    public Booking() {}

    public Booking(User customer, User provider, ServiceListing service, LocalDateTime bookingDateTime) {
        this.customer = customer;
        this.provider = provider;
        this.service = service;
        this.bookingDateTime = bookingDateTime;
        this.basePrice = service.getPrice();
        this.totalAmount = service.getPrice();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public ServiceListing getService() { return service; }
    public void setService(ServiceListing service) { this.service = service; }

    public LocalDateTime getBookingDateTime() { return bookingDateTime; }
    public void setBookingDateTime(LocalDateTime bookingDateTime) { this.bookingDateTime = bookingDateTime; }

    public String getCustomerNotes() { return customerNotes; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }

    public String getProviderNotes() { return providerNotes; }
    public void setProviderNotes(String providerNotes) { this.providerNotes = providerNotes; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public BigDecimal getTravelCharge() { return travelCharge; }
    public void setTravelCharge(BigDecimal travelCharge) { this.travelCharge = travelCharge; }

    public BigDecimal getAddonCharges() { return addonCharges; }
    public void setAddonCharges(BigDecimal addonCharges) { this.addonCharges = addonCharges; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getRescheduledAt() { return rescheduledAt; }
    public void setRescheduledAt(LocalDateTime rescheduledAt) { this.rescheduledAt = rescheduledAt; }

    public String getCancellationNotes() { return cancellationNotes; }
    public void setCancellationNotes(String cancellationNotes) { this.cancellationNotes = cancellationNotes; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public Boolean getIsRefunded() { return isRefunded; }
    public void setIsRefunded(Boolean isRefunded) { this.isRefunded = isRefunded; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public BigDecimal getAssignedProviderScore() { return assignedProviderScore; }
    public void setAssignedProviderScore(BigDecimal assignedProviderScore) { this.assignedProviderScore = assignedProviderScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getAddonIds() { return addonIds; }
    public void setAddonIds(String addonIds) { this.addonIds = addonIds; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    // Backward-compatible accessors used by existing controllers/DTO mapping
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
