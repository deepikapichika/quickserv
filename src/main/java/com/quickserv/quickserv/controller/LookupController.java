package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class LookupController {
    private final CategoryService categoryService;
    private final LocationService locationService;

    public LookupController(CategoryService categoryService, LocationService locationService) {
        this.categoryService = categoryService;
        this.locationService = locationService;
    }

    @GetMapping("/categories/all")
    public List<Map<String, Object>> getAllCategories() {
        return categoryService.getAllCategories().stream().map(this::toCategoryPayload).toList();
    }

    @GetMapping("/locations/all")
    public List<String> getAllLocations() {
        return locationService.getAllLocations();
    }

    private Map<String, Object> toCategoryPayload(Category category) {
        return Map.of("id", category.getId(), "name", category.getName());
    }
}
