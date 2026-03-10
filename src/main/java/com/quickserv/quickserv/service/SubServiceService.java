package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.SubService;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.SubServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubServiceService {

    @Autowired
    private SubServiceRepository subServiceRepository;

    // Get all sub-services
    public List<SubService> getAllSubServices() {
        return subServiceRepository.findAll();
    }

    // Get sub-services by category
    public List<SubService> getSubServicesByCategory(Category category) {
        return subServiceRepository.findByCategory(category);
    }

    // Get sub-services by category ID
    public List<SubService> getSubServicesByCategoryId(Long categoryId) {
        return subServiceRepository.findByCategoryId(categoryId);
    }

    // Get sub-service by ID
    public SubService getSubServiceById(Long id) {
        return subServiceRepository.findById(id).orElse(null);
    }

    // Check existing sub-service by category and name (ignore case)
    public Optional<SubService> findByCategoryIdAndNameIgnoreCase(Long categoryId, String name) {
        if (categoryId == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        return subServiceRepository.findByCategoryIdAndNameIgnoreCase(categoryId, name.trim());
    }

    // Save a sub-service
    public SubService saveSubService(SubService subService) {
        return subServiceRepository.save(subService);
    }

    // Delete a sub-service
    public void deleteSubService(Long id) {
        subServiceRepository.deleteById(id);
    }
}
