package com.quickserv.quickserv.dto.provider;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class ProviderCreateRequest {

    @NotNull(message = "user_id is required")
    private Long userId;

    @NotNull(message = "category_id is required")
    private Long categoryId;

    @Size(max = 2000, message = "experience must be at most 2000 characters")
    private String experience;

    @DecimalMin(value = "0.0", inclusive = true, message = "service_charge must be greater than or equal to 0")
    private BigDecimal serviceCharge;

    @Size(max = 255, message = "availability must be at most 255 characters")
    private String availability;

    private List<Long> serviceIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(BigDecimal serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public List<Long> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<Long> serviceIds) {
        this.serviceIds = serviceIds;
    }
}
