package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ProviderServiceOffering;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.SubService;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderServiceLocationService;
import com.quickserv.quickserv.service.ProviderServiceOfferingService;
import com.quickserv.quickserv.service.ServiceService;
import com.quickserv.quickserv.service.SubServiceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SubServiceService subServiceService;

    @Autowired
    private ProviderServiceLocationService providerServiceLocationService;

    @Autowired
    private ProviderServiceOfferingService providerServiceOfferingService;

    // ============= PROVIDER SECTION =============

    // Show all services for logged-in provider
    @GetMapping("/provider/services")
    public String listProviderServices(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<ServiceListing> services = serviceService.getServicesByProvider(provider);
        model.addAttribute("services", services);
        return "provider-services";
    }

    // Show form to add new service
    @GetMapping("/provider/services/new")
    public String showAddServiceForm(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("service", new ServiceListing());
        model.addAttribute("categories", categoryService.getProviderCategoryOptions());
        return "service-form";
    }

    // Save new service
    @PostMapping("/provider/services/save")
    public String saveService(@ModelAttribute ServiceListing service,
                              @RequestParam Long categoryId,
                              HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        Category category = categoryService.getCategoryById(categoryId);
        service.setProvider(provider);
        service.setCategory(category);
        service.setIsAvailable(true);

        serviceService.saveService(service);
        return "redirect:/provider/services";
    }

    // Show form to edit existing service
    @GetMapping("/provider/services/edit/{id}")
    public String showEditServiceForm(@PathVariable Long id, HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service == null || !service.getProvider().getId().equals(provider.getId())) {
            return "redirect:/provider/services";
        }

        model.addAttribute("service", service);
        model.addAttribute("categories", categoryService.getProviderCategoryOptions());
        return "service-form";
    }

    // Update existing service
    @PostMapping("/provider/services/update/{id}")
    public String updateService(@PathVariable Long id,
                                @ModelAttribute("service") ServiceListing formService,
                                @RequestParam Long categoryId,
                                HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing existingService = serviceService.getServiceById(id);
        if (existingService == null || !existingService.getProvider().getId().equals(provider.getId())) {
            return "redirect:/provider/services";
        }

        Category category = categoryService.getCategoryById(categoryId);

        existingService.setTitle(formService.getTitle());
        existingService.setDescription(formService.getDescription());
        existingService.setPrice(formService.getPrice());
        existingService.setPriceUnit(formService.getPriceUnit());
        existingService.setLocation(formService.getLocation());
        existingService.setImageUrl(formService.getImageUrl());
        existingService.setCategory(category);

        serviceService.saveService(existingService);
        return "redirect:/provider/services";
    }

    // Delete service
    @GetMapping("/provider/services/delete/{id}")
    public String deleteService(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service != null && service.getProvider().getId().equals(provider.getId())) {
            serviceService.deleteService(id);
        }
        return "redirect:/provider/services";
    }

    // Toggle availability
    @GetMapping("/provider/services/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service != null && service.getProvider().getId().equals(provider.getId())) {
            service.setIsAvailable(!service.getIsAvailable());
            serviceService.saveService(service);
        }
        return "redirect:/provider/services";
    }

    // ============= CUSTOMER SECTION =============

    // Browse all services (with optional filters)
    @GetMapping("/browse")
    public String browseServices(@RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Long subServiceId,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice,
                                 Model model,
                                 HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedLocation = location == null ? "" : location.trim();

        Category selectedCategoryObj = resolveCategory(categoryId, normalizedSearch);
        List<SubService> subServices = selectedCategoryObj == null
                ? null
                : subServiceService.getSubServicesByCategoryId(selectedCategoryObj.getId());

        SubService selectedSubService = null;
        if (subServiceId != null && subServiceId > 0) {
            selectedSubService = subServiceService.getSubServiceById(subServiceId);
            if (selectedSubService != null) {
                selectedCategoryObj = selectedSubService.getCategory();
                if (subServices == null) {
                    subServices = subServiceService.getSubServicesByCategoryId(selectedCategoryObj.getId());
                }
            }
        }

        String effectiveSearch = normalizedSearch;
        if (selectedSubService != null && effectiveSearch.isEmpty()) {
            effectiveSearch = selectedSubService.getName();
        }
        final String searchKeyword = effectiveSearch;

        boolean searchedSubServiceUnavailable = false;
        Set<Long> providersWithMatchingSubService = null;

        if (selectedCategoryObj != null && !searchKeyword.isEmpty()) {
            List<SubService> matchedSubServices = subServiceService.getSubServicesByCategoryId(selectedCategoryObj.getId()).stream()
                    .filter(ss -> containsIgnoreCase(ss.getName(), searchKeyword))
                    .toList();

            if (!matchedSubServices.isEmpty()) {
                providersWithMatchingSubService = matchedSubServices.stream()
                        .flatMap(ss -> providerServiceOfferingService.getOfferingsBySubService(ss).stream())
                        .filter(ProviderServiceOffering::getIsAvailable)
                        .map(offering -> offering.getProvider().getId())
                        .collect(Collectors.toSet());
            } else if (selectedSubService == null) {
                searchedSubServiceUnavailable = true;
            }
        }

        List<ServiceListing> services;
        if (selectedCategoryObj != null) {
            services = serviceService.getServicesByCategory(selectedCategoryObj);
        } else if (!searchKeyword.isEmpty()) {
            services = serviceService.searchServices(searchKeyword);
        } else {
            services = serviceService.getAvailableServices();
        }

        services = services.stream()
                .filter(ServiceListing::getIsAvailable)
                .toList();

        if (!searchKeyword.isEmpty() && !searchedSubServiceUnavailable) {
            final Set<Long> providerSet = providersWithMatchingSubService;
            services = services.stream()
                    .filter(s -> containsIgnoreCase(s.getTitle(), searchKeyword)
                            || containsIgnoreCase(s.getDescription(), searchKeyword)
                            || containsIgnoreCase(s.getCategory().getName(), searchKeyword)
                            || (providerSet != null && providerSet.contains(s.getProvider().getId())))
                    .toList();
        }

        if (!normalizedLocation.isEmpty()) {
            services = services.stream()
                    .filter(s -> containsIgnoreCase(s.getLocation(), normalizedLocation)
                            || providerServiceLocationService
                            .getActiveLocations(s.getProvider())
                            .stream()
                            .anyMatch(loc -> containsIgnoreCase(loc.getLocationName(), normalizedLocation)))
                    .toList();
        }

        if (minPrice != null) {
            services = services.stream()
                    .filter(s -> s.getPrice() != null && s.getPrice().compareTo(minPrice) >= 0)
                    .toList();
        }

        if (maxPrice != null) {
            services = services.stream()
                    .filter(s -> s.getPrice() != null && s.getPrice().compareTo(maxPrice) <= 0)
                    .toList();
        }

        if (searchedSubServiceUnavailable) {
            services = List.of();
        }

        model.addAttribute("services", services);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategory", selectedCategoryObj != null ? selectedCategoryObj.getId() : categoryId);
        model.addAttribute("selectedCategoryObj", selectedCategoryObj);
        model.addAttribute("subServices", subServices);
        model.addAttribute("selectedSubServiceId", selectedSubService != null ? selectedSubService.getId() : null);
        model.addAttribute("searchKeyword", normalizedSearch);
        model.addAttribute("searchLocation", normalizedLocation);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("searchedSubServiceUnavailable", searchedSubServiceUnavailable);
        model.addAttribute("user", user);

        return "browse-services";
    }

    private Category resolveCategory(Long categoryId, String search) {
        if (categoryId != null && categoryId > 0) {
            return categoryService.getCategoryById(categoryId);
        }

        if (search == null || search.isBlank()) {
            return null;
        }

        String normalizedSearch = search.toLowerCase(Locale.ROOT);
        return categoryService.getAllCategories().stream()
                .filter(category -> {
                    String name = category.getName() == null ? "" : category.getName().toLowerCase(Locale.ROOT);
                    return name.contains(normalizedSearch) || normalizedSearch.contains(name);
                })
                .findFirst()
                .orElse(null);
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null || query == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    // View single service details
    @GetMapping("/service/{id}")
    public String viewService(@PathVariable Long id,
                              Model model,
                              HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service == null) {
            return "redirect:/browse";
        }

        List<ProviderServiceOffering> providerSubServices = List.of();
        if (service.getCategory() != null) {
            providerSubServices = providerServiceOfferingService.getProviderOfferingsByCategory(
                    service.getProvider(),
                    service.getCategory().getId()
            ).stream().filter(ProviderServiceOffering::getIsAvailable).toList();
        }

        model.addAttribute("service", service);
        model.addAttribute("providerSubServices", providerSubServices);
        model.addAttribute("user", user);
        return "service-detail";
    }
}
