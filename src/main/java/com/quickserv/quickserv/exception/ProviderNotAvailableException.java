package com.quickserv.quickserv.exception;

/**
 * Exception thrown when no eligible providers are available
 */
public class ProviderNotAvailableException extends BookingException {

    private Long categoryId;
    private String location;
    private Double radius;

    public ProviderNotAvailableException(String message) {
        super(message, "NO_PROVIDER_AVAILABLE");
    }

    public ProviderNotAvailableException(Long categoryId, String location, Double radiusKm) {
        super(String.format(
            "No providers available for category %d in %s within %.1f km",
            categoryId, location, radiusKm
        ), "NO_PROVIDER_AVAILABLE");
        this.categoryId = categoryId;
        this.location = location;
        this.radius = radiusKm;
    }

    public Long getCategoryId() { return categoryId; }
    public String getLocation() { return location; }
    public Double getRadius() { return radius; }
}

