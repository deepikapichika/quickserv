package com.quickserv.quickserv.dto.booking;

import java.math.BigDecimal;

/**
 * DTO for detailed pricing breakdown of a booking
 */
public class PricingBreakdownDto {

    private BigDecimal basePrice;
    private BigDecimal travelCharge;
    private BigDecimal addonCharges;
    private BigDecimal couponDiscount;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private BigDecimal distanceKm;
    private String couponCode;
    private String description;

    // Constructors
    public PricingBreakdownDto() {
    }

    public PricingBreakdownDto(BigDecimal basePrice, BigDecimal travelCharge,
                              BigDecimal addonCharges, BigDecimal couponDiscount,
                              BigDecimal gstAmount, BigDecimal totalAmount) {
        this.basePrice = basePrice;
        this.travelCharge = travelCharge;
        this.addonCharges = addonCharges;
        this.couponDiscount = couponDiscount;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
        this.subtotal = basePrice.add(travelCharge).add(addonCharges).subtract(couponDiscount);
    }

    // Getters and Setters
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public BigDecimal getTravelCharge() { return travelCharge; }
    public void setTravelCharge(BigDecimal travelCharge) { this.travelCharge = travelCharge; }

    public BigDecimal getAddonCharges() { return addonCharges; }
    public void setAddonCharges(BigDecimal addonCharges) { this.addonCharges = addonCharges; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Build human-readable pricing summary
     */
    public String getPricingSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Base Price: ₹").append(basePrice).append("\n");
        if (travelCharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Travel Charge: ₹").append(travelCharge).append("\n");
        }
        if (addonCharges.compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Add-on Charges: ₹").append(addonCharges).append("\n");
        }
        if (couponDiscount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Coupon Discount: -₹").append(couponDiscount).append("\n");
        }
        sb.append("Subtotal: ₹").append(subtotal).append("\n");
        sb.append("GST (18%): ₹").append(gstAmount).append("\n");
        sb.append("TOTAL: ₹").append(totalAmount);
        return sb.toString();
    }
}

