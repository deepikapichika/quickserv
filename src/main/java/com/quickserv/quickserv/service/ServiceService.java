package com.quickserv.quickserv.service;

import com.quickserv.quickserv.dto.search.ServiceSearchResultDto;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ProviderRepository providerRepository;

    // Get all services
    public List<ServiceListing> getAllServices() {
        return serviceRepository.findAll();
    }

    // Get only available services
    public List<ServiceListing> getAvailableServices() {
        return serviceRepository.findByIsAvailableTrue();
    }

    // Get services by provider
    public List<ServiceListing> getServicesByProvider(User provider) {
        return serviceRepository.findByProvider(provider);
    }

    // Get services by category
    public List<ServiceListing> getServicesByCategory(Category category) {
        return serviceRepository.findByCategory(category);
    }

    // Search services by keyword
    public List<ServiceListing> searchServices(String keyword) {
        return serviceRepository.searchServices(keyword);
    }

    // Get one service by ID
    public ServiceListing getServiceById(Long id) {
        return serviceRepository.findById(id).orElse(null);
    }

    // Get one service by ID as Optional
    public java.util.Optional<ServiceListing> getServiceByIdAsOptional(Long id) {
        return serviceRepository.findById(id);
    }

    // Save a service
    public ServiceListing saveService(ServiceListing service) {
        return serviceRepository.save(service);
    }

    // Delete a service
    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    // Toggle availability
    public ServiceListing toggleAvailability(Long id) {
        ServiceListing service = getServiceById(id);
        if (service != null) {
            service.setIsAvailable(!service.getIsAvailable());
            return serviceRepository.save(service);
        }
        return null;
    }

    public List<ServiceSearchResultDto> searchDiscovery(String location, Long categoryId, BigDecimal maxPrice) {
        return searchDiscovery(location, categoryId, null, null, maxPrice, null, null, null);
    }

    public List<ServiceSearchResultDto> searchDiscovery(String location,
                                                        Long categoryId,
                                                        Long subcategoryId,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        Double minRating) {
        return searchDiscovery(location, categoryId, subcategoryId, minPrice, maxPrice, minRating, null, null);
    }

    public List<ServiceSearchResultDto> searchDiscovery(String location,
                                                        Long categoryId,
                                                        Long subcategoryId,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        Double minRating,
                                                        BigDecimal customerLatitude,
                                                        BigDecimal customerLongitude) {
        List<ServiceSearchResultDto> results = serviceRepository.searchDiscovery(
                cleanNullable(location),
                categoryId,
                subcategoryId,
                minPrice,
                maxPrice,
                minRating
        );
        return filterNearbySearchResults(results, customerLatitude, customerLongitude);
    }

    public List<Map<String, Object>> getServicesForCategoryLookup(Long categoryId) {
        return serviceRepository.findForCategoryLookup(categoryId).stream()
                .map(service -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", service.getId());
                    payload.put("name", service.getTitle());
                    payload.put("price", service.getPrice());
                    return payload;
                })
                .toList();
    }

    public void assignServicesToProvider(User providerUser,
                                         Category category,
                                         List<Long> serviceIds,
                                         BigDecimal fallbackPrice) {
        if (providerUser == null || category == null) {
            return;
        }

        List<ServiceListing> templates = loadSelectedTemplates(category.getId(), serviceIds);
        if (templates.isEmpty()) {
            // No default service is created anymore
            // Providers must explicitly add services from their dashboard
            return;
        }

        List<ServiceListing> toCreate = new ArrayList<>();
        for (ServiceListing template : templates) {
            String title = template.getTitle() == null ? null : template.getTitle().trim();
            if (title == null || title.isEmpty()) {
                continue;
            }

            if (serviceRepository.existsByProviderAndCategoryAndTitleIgnoreCase(providerUser, category, title)) {
                continue;
            }

            ServiceListing listing = new ServiceListing();
            listing.setTitle(title);
            listing.setDescription(template.getDescription());
            listing.setProvider(providerUser);
            listing.setCategory(category);
            listing.setPrice(resolvePrice(template.getPrice(), fallbackPrice));
            listing.setPriceUnit(defaultIfBlank(template.getPriceUnit(), "per service"));
            String providerLocation = defaultIfBlank(providerUser.getLocation(), "Available on request");
            listing.setLocation(providerLocation);
            listing.setServiceLocations(providerLocation);
            listing.setImageUrl(template.getImageUrl());
            listing.setIsAvailable(true);
            toCreate.add(listing);
        }

        if (!toCreate.isEmpty()) {
            serviceRepository.saveAll(toCreate);
        }
    }

    private List<ServiceListing> loadSelectedTemplates(Long categoryId, List<Long> serviceIds) {
        if (categoryId == null || serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = serviceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return serviceRepository.findByCategoryIdAndIdIn(categoryId, distinctIds);
    }

    private void createDefaultService(User providerUser, Category category, BigDecimal fallbackPrice) {
        if (serviceRepository.existsByProviderAndCategoryAndTitleIgnoreCase(providerUser, category, category.getName())) {
            return;
        }

        ServiceListing listing = new ServiceListing();
        listing.setTitle(category.getName());
        listing.setDescription("Service offering in " + category.getName());
        listing.setProvider(providerUser);
        listing.setCategory(category);
        listing.setPrice(resolvePrice(null, fallbackPrice));
        listing.setPriceUnit("per service");
        String providerLocation = defaultIfBlank(providerUser.getLocation(), "Available on request");
        listing.setLocation(providerLocation);
        listing.setServiceLocations(providerLocation);
        listing.setIsAvailable(true);
        serviceRepository.save(listing);
    }

    private BigDecimal resolvePrice(BigDecimal templatePrice, BigDecimal fallbackPrice) {
        if (templatePrice != null && templatePrice.compareTo(BigDecimal.ZERO) > 0) {
            return templatePrice;
        }
        if (fallbackPrice != null && fallbackPrice.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackPrice;
        }
        return BigDecimal.valueOf(300);
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

    public List<ServiceListing> browseServices(String keyword,
                                               String location,
                                               Long categoryId,
                                               Long subcategoryId,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice) {
        return browseServices(keyword, location, categoryId, subcategoryId, minPrice, maxPrice, null, null);
    }

    public List<ServiceListing> browseServices(String keyword,
                                               String location,
                                               Long categoryId,
                                               Long subcategoryId,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice,
                                               BigDecimal customerLatitude,
                                               BigDecimal customerLongitude) {
        List<ServiceListing> services = serviceRepository.browseWithFilters(
                cleanNullable(keyword),
                cleanNullable(location),
                categoryId,
                subcategoryId,
                minPrice,
                maxPrice
        );
        return filterNearbyServices(services, customerLatitude, customerLongitude);
    }

    private List<ServiceListing> filterNearbyServices(List<ServiceListing> services,
                                                      BigDecimal customerLatitude,
                                                      BigDecimal customerLongitude) {
        if (!hasCoordinates(customerLatitude, customerLongitude) || services == null || services.isEmpty()) {
            return services;
        }

        Map<Long, Provider> providerByUserId = providerRepository.findAll().stream()
                .filter(provider -> provider.getUser() != null)
                .collect(java.util.stream.Collectors.toMap(
                        provider -> provider.getUser().getId(),
                        provider -> provider,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ServiceListing> nearby = new ArrayList<>();
        for (ServiceListing service : services) {
            Provider provider = service != null && service.getProvider() != null
                    ? providerByUserId.get(service.getProvider().getId())
                    : null;
            if (provider == null || !hasCoordinates(provider.getLatitude(), provider.getLongitude())) {
                continue;
            }
            double distanceKm = calculateDistanceKm(
                    customerLatitude.doubleValue(),
                    customerLongitude.doubleValue(),
                    provider.getLatitude().doubleValue(),
                    provider.getLongitude().doubleValue()
            );
            if (distanceKm <= 10.0d) {
                nearby.add(service);
            }
        }

        return nearby.isEmpty() ? services : nearby;
    }

    private List<ServiceSearchResultDto> filterNearbySearchResults(List<ServiceSearchResultDto> results,
                                                                   BigDecimal customerLatitude,
                                                                   BigDecimal customerLongitude) {
        if (!hasCoordinates(customerLatitude, customerLongitude) || results == null || results.isEmpty()) {
            return results;
        }

        Map<Long, Provider> providerById = providerRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Provider::getProviderId,
                        provider -> provider,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ServiceSearchResultDto> nearby = new ArrayList<>();
        for (ServiceSearchResultDto result : results) {
            Provider provider = providerById.get(result.getProviderId());
            if (provider == null || !hasCoordinates(provider.getLatitude(), provider.getLongitude())) {
                continue;
            }
            double distanceKm = calculateDistanceKm(
                    customerLatitude.doubleValue(),
                    customerLongitude.doubleValue(),
                    provider.getLatitude().doubleValue(),
                    provider.getLongitude().doubleValue()
            );
            if (distanceKm <= 10.0d) {
                nearby.add(result);
            }
        }

        return nearby.isEmpty() ? results : nearby;
    }

    private boolean hasCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null && longitude != null;
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
