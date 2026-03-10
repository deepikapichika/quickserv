package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.ProviderServiceLocation;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.ProviderServiceLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderServiceLocationService {

    @Autowired
    private ProviderServiceLocationRepository repository;

    public ProviderServiceLocation addLocation(User provider, String locationName, String address) {
        String normalizedLocation = locationName == null ? "" : locationName.trim();
        if (normalizedLocation.isEmpty()) {
            throw new RuntimeException("Location is required.");
        }
        ensureDefaultPrimaryLocation(provider);
        if (repository.existsByProviderAndLocationNameIgnoreCase(provider, normalizedLocation)) {
            throw new RuntimeException("This location already exists for this provider.");
        }
        ProviderServiceLocation location = new ProviderServiceLocation(provider, normalizedLocation, false);
        location.setAddress(address == null ? null : address.trim());
        return repository.save(location);
    }

    public ProviderServiceLocation updateLocation(Long locationId, String locationName, String address, Boolean isActive) {
        ProviderServiceLocation location = repository.findById(locationId)
            .orElseThrow(() -> new RuntimeException("Location not found."));
        String normalizedLocation = locationName == null ? "" : locationName.trim();
        if (normalizedLocation.isEmpty()) {
            throw new RuntimeException("Location is required.");
        }
        location.setLocationName(normalizedLocation);
        location.setAddress(address == null ? null : address.trim());
        location.setIsActive(isActive != null ? isActive : Boolean.FALSE);
        return repository.save(location);
    }

    public void deleteLocation(Long locationId) {
        repository.deleteById(locationId);
    }

    public List<ProviderServiceLocation> getProviderLocations(User provider) {
        ensureDefaultPrimaryLocation(provider);
        return repository.findByProvider(provider);
    }

    public List<ProviderServiceLocation> getActiveLocations(User provider) {
        ensureDefaultPrimaryLocation(provider);
        return repository.findByProviderAndIsActiveTrue(provider);
    }

    public ProviderServiceLocation getPrimaryLocation(User provider) {
        ensureDefaultPrimaryLocation(provider);
        return repository.findByProviderAndIsPrimaryTrue(provider).orElse(null);
    }

    public ProviderServiceLocation getLocationById(Long locationId) {
        return repository.findById(locationId).orElse(null);
    }

    public void setPrimaryLocation(User provider, Long locationId) {
        ProviderServiceLocation newPrimary = getLocationById(locationId);
        if (newPrimary == null || !newPrimary.getProvider().getId().equals(provider.getId())) {
            return;
        }

        ProviderServiceLocation currentPrimary = getPrimaryLocation(provider);
        if (currentPrimary != null && !currentPrimary.getId().equals(locationId)) {
            currentPrimary.setIsPrimary(false);
            repository.save(currentPrimary);
        }

        newPrimary.setIsPrimary(true);
        newPrimary.setIsActive(true);
        repository.save(newPrimary);
    }

    private void ensureDefaultPrimaryLocation(User provider) {
        if (provider == null || provider.getId() == null) {
            return;
        }
        if (repository.findByProviderAndIsPrimaryTrue(provider).isPresent()) {
            return;
        }

        String providerLocation = provider.getLocation() == null ? "" : provider.getLocation().trim();
        if (providerLocation.isEmpty()) {
            return;
        }

        ProviderServiceLocation primary = repository.findByProviderAndLocationNameIgnoreCase(provider, providerLocation)
                .orElse(new ProviderServiceLocation(provider, providerLocation, true));
        primary.setIsPrimary(true);
        primary.setIsActive(true);
        repository.save(primary);
    }
}
