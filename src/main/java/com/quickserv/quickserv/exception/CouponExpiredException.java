package com.quickserv.quickserv.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when a coupon is invalid or expired
 */
public class CouponExpiredException extends BookingException {

    private String couponCode;
    private LocalDateTime expiryDate;

    public CouponExpiredException(String couponCode) {
        super("Coupon " + couponCode + " is expired or invalid", "COUPON_EXPIRED");
        this.couponCode = couponCode;
    }

    public CouponExpiredException(String couponCode, LocalDateTime expiryDate) {
        super("Coupon " + couponCode + " expired on " + expiryDate, "COUPON_EXPIRED");
        this.couponCode = couponCode;
        this.expiryDate = expiryDate;
    }

    public String getCouponCode() { return couponCode; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
}

