package com.quickserv.quickserv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

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

    // Constructors
    public Provider() {}

    public Provider(User user, Category category, BigDecimal serviceCharge) {
        this.user = user;
        this.category = category;
        this.serviceCharge = serviceCharge;
        this.availability = "Mon-Sun 9AM-6PM"; // Default availability
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
}
