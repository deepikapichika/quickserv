package com.quickserv.quickserv.dto.booking;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for service add-on (optional extras for booking)
 */
public class AddonDto {

    private Long id;

    @NotBlank(message = "Add-on name is required")
    @Size(max = 100, message = "Add-on name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    private Long serviceId;

    // Constructors
    public AddonDto() {
    }

    public AddonDto(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
}

