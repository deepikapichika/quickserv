package com.quickserv.quickserv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Long providerId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "experience")
    private String experience;

    @Column(name = "service_charge")
    private BigDecimal serviceCharge;

    @Column(name = "availability")
    private String availability; // e.g., "Mon-Fri 9AM-6PM"

    @Column(name = "rating")
    private Double rating = 0.0;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "provider_locations", length = 1000)
    private String providerLocations;

    @ManyToMany
    @JoinTable(
            name = "provider_categories",
            joinColumns = @JoinColumn(name = "provider_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> selectedCategories = new LinkedHashSet<>();

    // Constructors
    public Provider() {}

    public Provider(User user, Category category, BigDecimal serviceCharge) {
        this.user = user;
        this.category = category;
        this.serviceCharge = serviceCharge;
        this.availability = "Mon-Sun 9AM-6PM"; // Default availability
        if (category != null) {
            this.selectedCategories.add(category);
        }
    }

    // Getters and Setters
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public BigDecimal getServiceCharge() { return serviceCharge; }
    public void setServiceCharge(BigDecimal serviceCharge) { this.serviceCharge = serviceCharge; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public String getProviderLocations() { return providerLocations; }
    public void setProviderLocations(String providerLocations) { this.providerLocations = providerLocations; }

    public Set<Category> getSelectedCategories() { return selectedCategories; }
    public void setSelectedCategories(Set<Category> selectedCategories) { this.selectedCategories = selectedCategories; }
}
