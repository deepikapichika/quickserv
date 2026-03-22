package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Subcategory;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.LocationService;
import com.quickserv.quickserv.service.ServiceService;
import com.quickserv.quickserv.service.SubcategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class LookupController {
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final ServiceService serviceService;
    private final SubcategoryService subcategoryService;

    public LookupController(CategoryService categoryService,
                            LocationService locationService,
                            ServiceService serviceService,
                            SubcategoryService subcategoryService) {
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.serviceService = serviceService;
        this.subcategoryService = subcategoryService;
    }

    @GetMapping("/categories/all")
    public List<Map<String, Object>> getAllCategories() {
        return categoryService.getAllCategories().stream().map(this::toCategoryPayload).toList();
    }

    @GetMapping("/locations/all")
    public List<String> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/services/by-category/{categoryId}")
    public List<Map<String, Object>> getServicesByCategory(@PathVariable Long categoryId) {
        return serviceService.getServicesForCategoryLookup(categoryId);
    }

    @GetMapping("/subcategories/all")
    public List<Map<String, Object>> getAllSubcategories() {
        return subcategoryService.getAllSubcategories().stream().map(this::toSubcategoryPayload).toList();
    }

    @GetMapping("/subcategories/by-category/{categoryId}")
    public List<Map<String, Object>> getSubcategoriesByCategory(@PathVariable Long categoryId) {
        return subcategoryService.getByCategory(categoryId).stream().map(this::toSubcategoryPayload).toList();
    }

    private Map<String, Object> toCategoryPayload(Category category) {
        return Map.of("id", category.getId(), "name", category.getName());
    }

    private Map<String, Object> toSubcategoryPayload(Subcategory subcategory) {
        return Map.of(
                "id", subcategory.getId(),
                "name", subcategory.getName(),
                "categoryId", subcategory.getCategory().getId()
        );
    }
}
