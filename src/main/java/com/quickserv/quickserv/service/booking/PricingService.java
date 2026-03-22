package com.quickserv.quickserv.service.booking;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.exception.BusinessValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating booking prices
 * Handles: base price, distance charges, add-ons, coupons, GST
 *
 * Pricing Formula:
 * Subtotal = BasePrice + TravelCharge + AddonCharges - CouponDiscount
 * GST (18%) = Subtotal * 0.18
 * TotalAmount = Subtotal + GST
 */
@Service
public class PricingService {

    private static final BigDecimal TRAVEL_CHARGE_PER_KM = BigDecimal.valueOf(5);
    private static final BigDecimal FREE_TRAVEL_KM = BigDecimal.valueOf(5);
    private static final BigDecimal GST_PERCENTAGE = BigDecimal.valueOf(18);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Calculate distance-based travel charge
     * Formula: ₹5 per km for distance beyond 5km
     *
     * @param distanceKm Distance in kilometers
     * @return Travel charge amount
     */
    public BigDecimal calculateTravelCharge(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(FREE_TRAVEL_KM) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal chargeableDistance = distanceKm.subtract(FREE_TRAVEL_KM);
        return chargeableDistance.multiply(TRAVEL_CHARGE_PER_KM)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate GST (18%) on subtotal
     *
     * @param subtotal Amount before GST
     * @return GST amount
     */
    public BigDecimal calculateGST(BigDecimal subtotal) {
        return subtotal.multiply(GST_PERCENTAGE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total booking amount with all charges
     *
     * @param booking Booking entity with pricing details
     * @return Updated booking with calculated total
     */
    public Booking calculateTotalAmount(Booking booking) {
        // Validate inputs
        if (booking.getBasePrice() == null || booking.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Base price must be greater than 0");
        }

        // Calculate travel charge if distance is provided
        BigDecimal travelCharge = BigDecimal.ZERO;
        if (booking.getDistanceKm() != null && booking.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            travelCharge = calculateTravelCharge(booking.getDistanceKm());
            booking.setTravelCharge(travelCharge);
        }

        // Get addon charges (should be pre-calculated)
        BigDecimal addonCharges = booking.getAddonCharges() != null ?
            booking.getAddonCharges() : BigDecimal.ZERO;

        // Get coupon discount (should be validated before)
        BigDecimal couponDiscount = booking.getCouponDiscount() != null ?
            booking.getCouponDiscount() : BigDecimal.ZERO;

        // Calculate subtotal
        BigDecimal subtotal = booking.getBasePrice()
            .add(travelCharge)
            .add(addonCharges)
            .subtract(couponDiscount);

        // Ensure subtotal is not negative
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            subtotal = BigDecimal.ZERO;
        }

        // Calculate GST
        BigDecimal gst = calculateGST(subtotal);
        booking.setGstAmount(gst);

        // Calculate final total
        BigDecimal total = subtotal.add(gst).setScale(2, RoundingMode.HALF_UP);
        booking.setTotalAmount(total);

        return booking;
    }

    /**
     * Apply percentage coupon discount
     *
     * @param amount Amount to apply discount on
     * @param discountPercentage Discount percentage (e.g., 10 for 10%)
     * @param maxDiscount Maximum discount cap (if any)
     * @return Discount amount
     */
    public BigDecimal applyPercentageDiscount(BigDecimal amount, BigDecimal discountPercentage,
                                             BigDecimal maxDiscount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = amount.multiply(discountPercentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // Apply max discount cap if specified
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    /**
     * Apply fixed amount coupon discount
     *
     * @param amount Amount to apply discount on
     * @param fixedDiscount Fixed discount amount
     * @return Discount amount (capped at the amount if needed)
     */
    public BigDecimal applyFixedDiscount(BigDecimal amount, BigDecimal fixedDiscount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (fixedDiscount == null || fixedDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Discount cannot exceed the amount
        if (fixedDiscount.compareTo(amount) > 0) {
            return amount;
        }

        return fixedDiscount;
    }

    /**
     * Validate coupon is applicable for this booking amount
     *
     * @param bookingAmount Total booking amount
     * @param minAmount Minimum amount required for coupon
     * @return true if coupon is valid for this amount
     */
    public boolean isMinimumAmountMet(BigDecimal bookingAmount, BigDecimal minAmount) {
        if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return bookingAmount != null && bookingAmount.compareTo(minAmount) >= 0;
    }

    /**
     * Calculate add-on charges by summing individual add-on prices
     *
     * @param addonPrices Array of add-on prices
     * @return Total add-on charges
     */
    public BigDecimal calculateAddonCharges(BigDecimal... addonPrices) {
        BigDecimal total = BigDecimal.ZERO;

        if (addonPrices != null) {
            for (BigDecimal price : addonPrices) {
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(price);
                }
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}

