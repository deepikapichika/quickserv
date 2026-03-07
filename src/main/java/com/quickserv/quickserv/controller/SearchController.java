package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.service.ProviderService;
import com.quickserv.quickserv.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/services")
public class SearchController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private CategoryService categoryService;

    // Advanced search with multiple filters
    @GetMapping("/search")
    public String searchServices(@RequestParam(required = false) String location,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Double minRating,
                               Model model) {

        List<Provider> providers;

        // Apply filters based on parameters
        if (categoryId != null && location != null && !location.trim().isEmpty()) {
            // Search by both category and location
            providers = providerService.getProvidersByCategoryAndLocation(categoryId, location);
        } else if (categoryId != null) {
            // Search by category only
            providers = providerService.getProvidersByCategory(categoryId);
        } else if (location != null && !location.trim().isEmpty()) {
            // Search by location only
            providers = providerService.getProvidersByLocation(location);
        } else {
            // No filters - return all providers
            providers = providerService.getAllProviders();
        }

        // Filter by minimum rating if specified
        if (minRating != null && minRating > 0) {
            providers = providers.stream()
                    .filter(p -> p.getRating() >= minRating)
                    .toList();
        }

        // Get all categories for the filter dropdown
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("providers", providers);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("minRating", minRating);

        return "search-results";
    }

    // Quick search endpoint (for AJAX calls)
    @GetMapping("/quick-search")
    @ResponseBody
    public List<Provider> quickSearch(@RequestParam(required = false) String query,
                                    @RequestParam(required = false) Long categoryId) {
        if (query != null && !query.trim().isEmpty()) {
            // Search by location
            return providerService.getProvidersByLocation(query);
        } else if (categoryId != null) {
            // Search by category
            return providerService.getProvidersByCategory(categoryId);
        } else {
            // Return top rated providers
            return providerService.getAllProviders().stream()
                    .filter(p -> p.getRating() >= 4.0)
                    .limit(10)
                    .toList();
        }
    }
}
