package com.quickserv.quickserv.service.booking;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.exception.BusinessValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for handling booking cancellations and refunds
 * Implements cancellation policy:
 * - >24 hours before: Full refund (100%)
 * - 12-24 hours before: Partial refund (50%)
 * - <12 hours before: No refund (0%)
 * - Provider no-show: Full refund + ₹100 credit
 */
@Service
public class CancellationService {

    private static final int FULL_REFUND_HOURS = 24;
    private static final int PARTIAL_REFUND_MIN_HOURS = 12;
    private static final double PARTIAL_REFUND_PERCENT = 0.50;
    private static final BigDecimal NOSHOW_CREDIT = BigDecimal.valueOf(100);

    /**
     * Calculate refund amount based on cancellation policy
     *
     * @param booking Booking to calculate refund for
     * @return Refund amount in rupees
     */
    public BigDecimal calculateRefund(Booking booking) {
        if (booking == null || booking.getTotalAmount() == null) {
            throw new BusinessValidationException("Invalid booking for refund calculation");
        }

        if (!booking.canBeCancelled()) {
            throw new BusinessValidationException(
                "Booking cannot be cancelled in " + booking.getStatus() + " state"
            );
        }

        long hoursUntilService = calculateHoursUntilService(booking.getBookingDateTime());

        // >24 hours before service
        if (hoursUntilService > FULL_REFUND_HOURS) {
            return booking.getTotalAmount();
        }

        // 12-24 hours before service
        if (hoursUntilService > PARTIAL_REFUND_MIN_HOURS) {
            return booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(PARTIAL_REFUND_PERCENT));
        }

        // <12 hours before service
        return BigDecimal.ZERO;
    }

    /**
     * Calculate refund for provider no-show
     * Full refund + ₹100 credit
     *
     * @param booking Booking with provider no-show
     * @return Refund amount + credit
     */
    public BigDecimal calculateNoShowRefund(Booking booking) {
        if (booking == null || booking.getTotalAmount() == null) {
            throw new BusinessValidationException("Invalid booking for no-show refund");
        }

        return booking.getTotalAmount().add(NOSHOW_CREDIT);
    }

    /**
     * Process cancellation with refund
     *
     * @param booking Booking to cancel
     * @param reason Cancellation reason
     * @param isProviderNoShow Whether provider failed to show up
     * @return Updated booking with refund details
     */
    public Booking processCancellation(Booking booking, String reason, boolean isProviderNoShow) {
        if (!booking.canBeCancelled()) {
            throw new BusinessValidationException("Cannot cancel booking in " + booking.getStatus() + " state");
        }

        // Calculate refund
        BigDecimal refundAmount = isProviderNoShow ?
            calculateNoShowRefund(booking) :
            calculateRefund(booking);

        // Update booking
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setRefundAmount(refundAmount);
        booking.setCancellationNotes(reason);

        if (isProviderNoShow) {
            booking.setRefundReason("Provider no-show - Full refund + ₹100 credit");
        } else {
            booking.setRefundReason(buildRefundReason(refundAmount, booking.getTotalAmount()));
        }

        return booking;
    }

    /**
     * Mark booking refund as processed
     *
     * @param booking Booking to mark as refunded
     * @return Updated booking
     */
    public Booking markRefundProcessed(Booking booking) {
        if (booking.getRefundAmount() == null || booking.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("No refund to process for this booking");
        }

        booking.setIsRefunded(true);
        return booking;
    }

    /**
     * Calculate hours until service
     *
     * @param serviceDateTime Booking date and time
     * @return Hours remaining until service
     */
    private long calculateHoursUntilService(LocalDateTime serviceDateTime) {
        LocalDateTime now = LocalDateTime.now();
        return java.time.temporal.ChronoUnit.HOURS.between(now, serviceDateTime);
    }

    /**
     * Build human-readable refund reason
     *
     * @param refundAmount Amount being refunded
     * @param totalAmount Total booking amount
     * @return Refund reason description
     */
    private String buildRefundReason(BigDecimal refundAmount, BigDecimal totalAmount) {
        BigDecimal refundPercent = refundAmount.multiply(BigDecimal.valueOf(100))
            .divide(totalAmount, 0, java.math.RoundingMode.HALF_UP);

        if (refundPercent.compareTo(BigDecimal.valueOf(100)) == 0) {
            return "Full refund - Cancellation >24 hours before service";
        } else if (refundPercent.compareTo(BigDecimal.ZERO) > 0) {
            return "Partial refund (" + refundPercent + "%) - Cancellation 12-24 hours before service";
        } else {
            return "No refund - Cancellation <12 hours before service";
        }
    }

    /**
     * Check if booking can be cancelled
     *
     * @param booking Booking to check
     * @return true if cancellation is allowed
     */
    public boolean canCancel(Booking booking) {
        return booking != null && booking.canBeCancelled();
    }

    /**
     * Get refund policy description
     *
     * @return Policy description
     */
    public String getRefundPolicyDescription() {
        return "Refund Policy:\n" +
               "- >24 hours before service: 100% refund\n" +
               "- 12-24 hours before service: 50% refund\n" +
               "- <12 hours before service: No refund\n" +
               "- Provider no-show: 100% refund + ₹100 credit";
    }
}

