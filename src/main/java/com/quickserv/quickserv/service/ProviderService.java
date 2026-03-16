package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    // Register a new provider
    public Provider registerProvider(User user, Long categoryId, BigDecimal serviceCharge, CategoryService categoryService) {
        // Check if user is already a provider
        if (providerRepository.findByUser(user) != null) {
            throw new RuntimeException("User is already registered as a provider");
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new RuntimeException("Invalid category");
        }

        Provider provider = new Provider(user, category, serviceCharge);
        provider.setSelectedCategories(new LinkedHashSet<>(List.of(category)));
        provider.setProviderLocations(user.getLocation());
        return providerRepository.save(provider);
    }

    public Provider registerProviderFromRegistration(User user,
                                                     Long categoryId,
                                                     CategoryService categoryService) {
        if (providerRepository.findByUser(user) != null) {
            throw new RuntimeException("User is already registered as a provider");
        }

        if (categoryId == null) {
            throw new RuntimeException("Please select a service category");
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new RuntimeException("Invalid category selection");
        }

        Provider provider = new Provider(user, category, BigDecimal.ZERO);
        provider.setSelectedCategories(new LinkedHashSet<>(List.of(category)));
        provider.setProviderLocations(user.getLocation());
        provider.setAvailability("Flexible");

        return providerRepository.save(provider);
    }

    // Get provider by user
    public Provider getProviderByUser(User user) {
        return providerRepository.findByUser(user);
    }

    // Get provider by ID
    public Provider getProviderById(Long id) {
        return providerRepository.findById(id).orElse(null);
    }

    // Get all providers
    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }

    // Get providers by category
    public List<Provider> getProvidersByCategory(Long categoryId) {
        return providerRepository.findByCategoryId(categoryId);
    }

    // Get providers by location
    public List<Provider> getProvidersByLocation(String location) {
        return providerRepository.findByLocation(location);
    }

    // Get providers by category and location
    public List<Provider> getProvidersByCategoryAndLocation(Long categoryId, String location) {
        return providerRepository.findByCategoryAndLocation(categoryId, location);
    }

    // Update provider profile
    public Provider updateProviderProfile(Long providerId, String experience, String availability, BigDecimal serviceCharge) {
        Provider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            throw new RuntimeException("Provider not found");
        }

        if (experience != null) provider.setExperience(experience);
        if (availability != null) provider.setAvailability(availability);
        if (serviceCharge != null) provider.setServiceCharge(serviceCharge);

        return providerRepository.save(provider);
    }

    // Update provider rating
    public void updateProviderRating(Long providerId, Double newRating) {
        Provider provider = providerRepository.findById(providerId).orElse(null);
        if (provider != null) {
            // Calculate new average rating
            double currentTotal = provider.getRating() * provider.getTotalReviews();
            int newTotalReviews = provider.getTotalReviews() + 1;
            double newAverageRating = (currentTotal + newRating) / newTotalReviews;

            provider.setRating(Math.round(newAverageRating * 10.0) / 10.0); // Round to 1 decimal
            provider.setTotalReviews(newTotalReviews);
            providerRepository.save(provider);
        }
    }

    // Delete provider
    public void deleteProvider(Long providerId) {
        providerRepository.deleteById(providerId);
    }
}
