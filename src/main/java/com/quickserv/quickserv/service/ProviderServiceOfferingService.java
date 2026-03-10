package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.ProviderServiceOffering;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.entity.SubService;
import com.quickserv.quickserv.repository.ProviderServiceOfferingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProviderServiceOfferingService {

    @Autowired
    private ProviderServiceOfferingRepository repository;

    public ProviderServiceOffering addServiceOffering(User provider, SubService subService, BigDecimal price, String priceUnit) {
        if (repository.existsByProviderAndSubService(provider, subService)) {
            throw new RuntimeException("This provider already offers this sub-service.");
        }
        ProviderServiceOffering offering = new ProviderServiceOffering(provider, subService, price, priceUnit);
        return repository.save(offering);
    }

    public ProviderServiceOffering addOrUpdateServiceOffering(User provider,
                                                              SubService subService,
                                                              BigDecimal price,
                                                              String priceUnit,
                                                              String description,
                                                              boolean isAvailable) {
        ProviderServiceOffering offering = repository.findByProviderAndSubService(provider, subService)
                .orElse(new ProviderServiceOffering(provider, subService, price, priceUnit));
        offering.setPrice(price);
        offering.setPriceUnit(priceUnit == null || priceUnit.isBlank() ? "per visit" : priceUnit);
        offering.setDescription(description);
        offering.setIsAvailable(isAvailable);
        return repository.save(offering);
    }

    public ProviderServiceOffering updateServiceOffering(Long offeringId, BigDecimal price, String priceUnit, String description, Boolean isAvailable) {
        ProviderServiceOffering offering = repository.findById(offeringId)
            .orElseThrow(() -> new RuntimeException("Service offering not found."));
        offering.setPrice(price);
        offering.setPriceUnit(priceUnit);
        offering.setDescription(description);
        offering.setIsAvailable(isAvailable);
        return repository.save(offering);
    }

    public void deleteServiceOffering(Long offeringId) {
        repository.deleteById(offeringId);
    }

    public List<ProviderServiceOffering> getProviderOfferings(User provider) {
        return repository.findByProvider(provider);
    }

    public ProviderServiceOffering getOfferingById(Long offeringId) {
        return repository.findById(offeringId).orElse(null);
    }

    public List<ProviderServiceOffering> getOfferingsBySubService(SubService subService) {
        return repository.findBySubService(subService);
    }

    public List<ProviderServiceOffering> getProviderOfferingsByCategory(User provider, Long categoryId) {
        if (provider == null || categoryId == null) {
            return List.of();
        }
        return repository.findByProviderAndSubService_Category_Id(provider, categoryId);
    }

    public List<ProviderServiceOffering> getAvailableOfferings() {
        return repository.findByIsAvailableTrue();
    }
}
