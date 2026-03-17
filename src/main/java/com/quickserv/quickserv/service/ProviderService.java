package com.quickserv.quickserv.service;

import com.quickserv.quickserv.dto.provider.ProviderCreateRequest;
import com.quickserv.quickserv.dto.provider.ProviderResponse;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ProviderService(ProviderRepository providerRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProviderResponse addProvider(ProviderCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for id: " + request.getUserId()));

        if (!"PROVIDER".equalsIgnoreCase(user.getRole())) {
            throw new BusinessValidationException("Only users with role PROVIDER can register as providers.");
        }

        if (providerRepository.existsByUser(user)) {
            throw new BusinessValidationException("User is already registered as a provider.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for id: " + request.getCategoryId()));

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setCategory(category);
        provider.setExperience(cleanNullable(request.getExperience()));
        provider.setServiceCharge(request.getServiceCharge() == null ? BigDecimal.ZERO : request.getServiceCharge());
        provider.setAvailability(defaultIfBlank(request.getAvailability(), "Flexible"));
        provider.setProviderLocations(cleanNullable(user.getLocation()));
        provider.setSelectedCategories(new LinkedHashSet<>(List.of(category)));

        Provider saved = providerRepository.save(provider);
        return toResponse(saved);
    }

    // Backward-compatible entry point used by current web flow.
    public Provider registerProvider(User user, Long categoryId, BigDecimal serviceCharge, CategoryService categoryService) {
        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setUserId(user.getId());
        request.setCategoryId(categoryId);
        request.setServiceCharge(serviceCharge);
        return providerRepository.findById(addProvider(request).getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider was not created."));
    }

    public Provider registerProviderFromRegistration(User user,
                                                     Long categoryId,
                                                     CategoryService categoryService) {
        if (categoryId == null) {
            throw new BusinessValidationException("Please select a service category.");
        }

        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setUserId(user.getId());
        request.setCategoryId(categoryId);
        request.setServiceCharge(BigDecimal.ZERO);
        request.setAvailability("Flexible");
        return providerRepository.findById(addProvider(request).getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider was not created."));
    }

    public Provider getProviderByUser(User user) {
        return providerRepository.findByUser(user);
    }

    public Provider getProviderById(Long id) {
        return providerRepository.findById(id).orElse(null);
    }

    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }

    public List<Provider> getProvidersByCategory(Long categoryId) {
        return providerRepository.findProvidersByCategoryId(categoryId);
    }

    public List<ProviderResponse> getProviderResponsesByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for id: " + categoryId));
        return providerRepository.findProvidersByCategoryId(category.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Provider> getProvidersByLocation(String location) {
        return providerRepository.findByLocation(location == null ? "" : location.trim());
    }

    public List<ProviderResponse> getProviderResponsesByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BusinessValidationException("Location must not be empty.");
        }
        return providerRepository.findByLocation(location.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Provider> getProvidersByCategoryAndLocation(Long categoryId, String location) {
        return providerRepository.findByCategoryAndLocation(categoryId, location == null ? "" : location.trim());
    }

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

    public void updateProviderRating(Long providerId, Double newRating) {
        Provider provider = providerRepository.findById(providerId).orElse(null);
        if (provider != null) {
            double currentTotal = provider.getRating() * provider.getTotalReviews();
            int newTotalReviews = provider.getTotalReviews() + 1;
            double newAverageRating = (currentTotal + newRating) / newTotalReviews;

            provider.setRating(Math.round(newAverageRating * 10.0) / 10.0);
            provider.setTotalReviews(newTotalReviews);
            providerRepository.save(provider);
        }
    }

    public void deleteProvider(Long providerId) {
        providerRepository.deleteById(providerId);
    }

    public ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setProviderId(provider.getProviderId());
        response.setUserId(provider.getUser() != null ? provider.getUser().getId() : null);
        response.setUserName(provider.getUser() != null ? provider.getUser().getName() : null);
        response.setUserEmail(provider.getUser() != null ? provider.getUser().getEmail() : null);
        response.setUserLocation(provider.getUser() != null ? provider.getUser().getLocation() : null);
        response.setCategoryId(provider.getCategory() != null ? provider.getCategory().getId() : null);
        response.setCategoryName(provider.getCategory() != null ? provider.getCategory().getName() : null);
        response.setExperience(provider.getExperience());
        response.setServiceCharge(provider.getServiceCharge());
        response.setAvailability(provider.getAvailability());
        response.setRating(provider.getRating());
        return response;
    }

    private String cleanNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
