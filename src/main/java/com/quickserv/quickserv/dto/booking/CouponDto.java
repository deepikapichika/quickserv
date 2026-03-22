package com.quickserv.quickserv.dto.booking;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for coupon/promo code management
 */
public class CouponDto {

    private Long id;

    @NotBlank(message = "Coupon code is required")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Coupon code must contain only uppercase letters and numbers")
    @Size(min = 3, max = 20, message = "Coupon code must be between 3 and 20 characters")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    @Pattern(regexp = "^(PERCENTAGE|FIXED)$", message = "Discount type must be PERCENTAGE or FIXED")
    private String discountType; // PERCENTAGE or FIXED

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", message = "Discount must be greater than or equal to 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Maximum discount cannot be negative")
    private BigDecimal maxDiscount; // For percentage discounts

    @DecimalMin(value = "0.0", message = "Minimum order amount must be greater than or equal to 0")
    private BigDecimal minOrderAmount; // Minimum booking amount to apply coupon

    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;

    @Min(value = 1, message = "Coupon must have at least 1 usage limit")
    private Integer usageLimit;

    private Integer usedCount = 0;

    private Boolean isActive = true;

    // Constructors
    public CouponDto() {
    }

    public CouponDto(String code, String description, String discountType, BigDecimal discountValue) {
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

