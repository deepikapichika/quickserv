package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

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
}
