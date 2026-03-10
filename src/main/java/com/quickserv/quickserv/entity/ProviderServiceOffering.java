package com.quickserv.quickserv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProviderServiceOffering: Links a provider with a specific sub-service and individual pricing.
 * This allows providers to offer multiple sub-services (e.g., Hair Cut, Hair Spa, Facial)
 * under their main service category, each with different pricing.
 */
@Entity
@Table(name = "provider_service_offerings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider_id", "sub_service_id"})
})
public class ProviderServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_service_id", nullable = false)
    private SubService subService;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String priceUnit; // "per hour", "per visit", "fixed"

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public ProviderServiceOffering() {}

    public ProviderServiceOffering(User provider, SubService subService, BigDecimal price, String priceUnit) {
        this.provider = provider;
        this.subService = subService;
        this.price = price;
        this.priceUnit = priceUnit;
        this.isAvailable = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getProvider() { return provider; }
    public void setProvider(User provider) { this.provider = provider; }

    public SubService getSubService() { return subService; }
    public void setSubService(SubService subService) { this.subService = subService; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

