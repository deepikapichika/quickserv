package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.*;
import com.quickserv.quickserv.service.SubServiceService;
import com.quickserv.quickserv.service.ProviderServiceOfferingService;
import com.quickserv.quickserv.service.ProviderServiceLocationService;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ServiceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/provider/offerings")
public class ProviderServiceOfferingController {

    @Autowired
    private ProviderServiceOfferingService offeringService;

    @Autowired
    private ProviderServiceLocationService locationService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SubServiceService subServiceService;

    @Autowired
    private ServiceService serviceService;

    @GetMapping
    public String listOfferings(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        Category primaryCategory = categoryService.resolveProviderPrimaryCategory(provider.getServiceType());
        List<ProviderServiceOffering> offerings = primaryCategory == null
                ? List.of()
                : offeringService.getProviderOfferings(provider).stream()
                        .filter(o -> o.getSubService() != null
                                && o.getSubService().getCategory() != null
                                && primaryCategory.getId().equals(o.getSubService().getCategory().getId()))
                        .toList();

        List<ServiceListing> customServices = primaryCategory == null
                ? List.of()
                : serviceService.getServicesByProvider(provider).stream()
                        .filter(s -> Boolean.TRUE.equals(s.getIsCustomService())
                                && s.getCategory() != null
                                && primaryCategory.getId().equals(s.getCategory().getId()))
                        .toList();

        List<ProviderServiceLocation> locations = locationService.getProviderLocations(provider);

        model.addAttribute("offerings", offerings);
        model.addAttribute("customServices", customServices);
        model.addAttribute("locations", locations);
        model.addAttribute("user", provider);
        model.addAttribute("primaryCategory", primaryCategory);

        return "provider-service-offerings";
    }

    @GetMapping("/add")
    public String showAddOfferingForm(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        populateAddOfferingModel(model, provider);
        return "provider-add-offering";
    }

    @PostMapping("/add")
    public String addOffering(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subServiceId,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String priceUnit,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<Long> subServiceIds,
            @RequestParam(required = false) List<BigDecimal> prices,
            @RequestParam(required = false) List<String> priceUnits,
            @RequestParam(required = false) List<String> descriptions,
            HttpSession session,
            Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        int addedCount = 0;
        int skippedCount = 0;

        Category primaryCategory = categoryService.resolveProviderPrimaryCategory(provider.getServiceType());
        if (primaryCategory == null) {
            populateAddOfferingModel(model, provider);
            model.addAttribute("error", "Please select a valid primary category in your profile first.");
            return "provider-add-offering";
        }

        try {
            // Backward-compatible path for older single-entry form submissions.
            if (subServiceIds == null || subServiceIds.isEmpty()) {
                SubService subService = subServiceService.getSubServiceById(subServiceId);
                if (subService == null) {
                    throw new RuntimeException("Sub-service not found.");
                }
                if (subService.getCategory() == null || !primaryCategory.getId().equals(subService.getCategory().getId())) {
                    throw new RuntimeException("You can only add sub-services from your primary category: " + primaryCategory.getName());
                }
                if (price == null || price.signum() <= 0) {
                    throw new RuntimeException("Please enter a valid price.");
                }

                ProviderServiceOffering offering = offeringService.addServiceOffering(
                        provider,
                        subService,
                        price,
                        (priceUnit == null || priceUnit.isBlank()) ? "per visit" : priceUnit
                );
                if (description != null && !description.isBlank()) {
                    offeringService.updateServiceOffering(
                            offering.getId(),
                            offering.getPrice(),
                            offering.getPriceUnit(),
                            description.trim(),
                            true
                    );
                }
                return "redirect:/provider/offerings?added=true&count=1";
            }

            for (int i = 0; i < subServiceIds.size(); i++) {
                Long currentSubServiceId = subServiceIds.get(i);
                if (currentSubServiceId == null) {
                    skippedCount++;
                    continue;
                }

                BigDecimal currentPrice = (prices != null && prices.size() > i) ? prices.get(i) : null;
                String currentPriceUnit = (priceUnits != null && priceUnits.size() > i) ? priceUnits.get(i) : "per visit";
                String currentDescription = (descriptions != null && descriptions.size() > i) ? descriptions.get(i) : null;

                if (currentPrice == null || currentPrice.signum() <= 0) {
                    skippedCount++;
                    continue;
                }

                SubService subService = subServiceService.getSubServiceById(currentSubServiceId);
                if (subService == null) {
                    skippedCount++;
                    continue;
                }
                if (subService.getCategory() == null || !primaryCategory.getId().equals(subService.getCategory().getId())) {
                    skippedCount++;
                    continue;
                }

                try {
                    ProviderServiceOffering offering = offeringService.addServiceOffering(
                            provider,
                            subService,
                            currentPrice,
                            (currentPriceUnit == null || currentPriceUnit.isBlank()) ? "per visit" : currentPriceUnit
                    );
                    if (currentDescription != null && !currentDescription.isBlank()) {
                        offeringService.updateServiceOffering(
                                offering.getId(),
                                offering.getPrice(),
                                offering.getPriceUnit(),
                                currentDescription.trim(),
                                true
                        );
                    }
                    addedCount++;
                } catch (RuntimeException ex) {
                    skippedCount++;
                }
            }

            if (addedCount > 0) {
                return "redirect:/provider/offerings?added=true&count=" + addedCount + "&skipped=" + skippedCount;
            }

            throw new RuntimeException("No sub-services were added. Check duplicate entries and price values.");
        } catch (RuntimeException e) {
            populateAddOfferingModel(model, provider);
            model.addAttribute("error", e.getMessage());
            return "provider-add-offering";
        }
    }

    @PostMapping("/add-custom")
    public String addCustomService(@RequestParam String serviceName,
                                   @RequestParam BigDecimal price,
                                   @RequestParam(required = false) String description,
                                   @RequestParam Integer duration,
                                   HttpSession session,
                                   Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            Category primaryCategory = categoryService.resolveProviderPrimaryCategory(provider.getServiceType());
            if (primaryCategory == null) {
                throw new RuntimeException("Please select a valid provider category in your profile first.");
            }
            if (serviceName == null || serviceName.isBlank()) {
                throw new RuntimeException("Service name is required.");
            }
            if (price == null || price.signum() <= 0) {
                throw new RuntimeException("Please enter a valid price.");
            }
            if (duration == null || duration <= 0) {
                throw new RuntimeException("Please enter a valid duration.");
            }

            ServiceListing custom = new ServiceListing();
            custom.setTitle(serviceName.trim());
            custom.setDescription(description == null ? "" : description.trim());
            custom.setPrice(price);
            custom.setPriceUnit("fixed");
            custom.setDurationMinutes(duration);
            custom.setProvider(provider);
            custom.setCategory(primaryCategory);
            custom.setLocation(provider.getLocation());
            custom.setIsAvailable(true);
            custom.setIsCustomService(true);

            serviceService.saveService(custom);
            return "redirect:/provider/offerings?customAdded=true";
        } catch (RuntimeException e) {
            populateAddOfferingModel(model, provider);
            model.addAttribute("error", e.getMessage());
            return "provider-add-offering";
        }
    }

    private void populateAddOfferingModel(Model model, User provider) {
        Category primaryCategory = categoryService.resolveProviderPrimaryCategory(provider.getServiceType());
        if (primaryCategory == null) {
            List<Category> options = categoryService.getProviderCategoryOptions();
            if (!options.isEmpty()) {
                primaryCategory = options.get(0);
            }
        }

        List<Category> orderedCategories = new java.util.ArrayList<>();
        if (primaryCategory != null) {
            orderedCategories.add(primaryCategory);
        }

        java.util.Map<Long, List<SubService>> subServicesByCategoryId = new java.util.LinkedHashMap<>();
        if (primaryCategory != null) {
            List<SubService> grouped = subServiceService.getSubServicesByCategoryId(primaryCategory.getId());
            if (grouped != null && !grouped.isEmpty()) {
                subServicesByCategoryId.put(primaryCategory.getId(), grouped);
            }
        }

        List<SubService> subServices = primaryCategory != null
                ? subServiceService.getSubServicesByCategoryId(primaryCategory.getId())
                : List.of();

        model.addAttribute("subServices", subServices);
        model.addAttribute("categories", orderedCategories);
        model.addAttribute("subServicesByCategoryId", subServicesByCategoryId);
        model.addAttribute("primaryCategory", primaryCategory);
        model.addAttribute("user", provider);
    }

    @GetMapping("/edit/{id}")
    public String showEditOfferingForm(@PathVariable Long id, HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ProviderServiceOffering offering = offeringService.getOfferingById(id);
        if (offering == null || !offering.getProvider().getId().equals(provider.getId())) {
            return "redirect:/provider/offerings";
        }

        model.addAttribute("offering", offering);
        model.addAttribute("user", provider);

        return "provider-edit-offering";
    }

    @PostMapping("/edit/{id}")
    public String updateOffering(
            @PathVariable Long id,
            @RequestParam BigDecimal price,
            @RequestParam String priceUnit,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "false") boolean isAvailable,
            HttpSession session,
            Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            ProviderServiceOffering offering = offeringService.getOfferingById(id);
            if (offering == null || !offering.getProvider().getId().equals(provider.getId())) {
                return "redirect:/provider/offerings";
            }

            offeringService.updateServiceOffering(id, price, priceUnit, description, isAvailable);
            return "redirect:/provider/offerings?updated=true";
        } catch (RuntimeException e) {
            ProviderServiceOffering offering = offeringService.getOfferingById(id);
            model.addAttribute("offering", offering);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", provider);

            return "provider-edit-offering";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteOffering(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ProviderServiceOffering offering = offeringService.getOfferingById(id);
        if (offering != null && offering.getProvider().getId().equals(provider.getId())) {
            offeringService.deleteServiceOffering(id);
        }

        return "redirect:/provider/offerings?deleted=true";
    }

    @GetMapping("/toggle/{id}")
    public String toggleOfferingAvailability(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ProviderServiceOffering offering = offeringService.getOfferingById(id);
        if (offering != null && offering.getProvider().getId().equals(provider.getId())) {
            offeringService.updateServiceOffering(
                id,
                offering.getPrice(),
                offering.getPriceUnit(),
                offering.getDescription(),
                !offering.getIsAvailable()
            );
        }

        return "redirect:/provider/offerings";
    }

    @GetMapping("/sub-services")
    @ResponseBody
    public List<SubService> getSubServices(@RequestParam(required = false) Long categoryId,
                                           HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return List.of();
        }

        Category primaryCategory = categoryService.resolveProviderPrimaryCategory(provider.getServiceType());
        if (primaryCategory == null) {
            return List.of();
        }

        Long effectiveCategoryId = primaryCategory.getId();
        if (categoryId != null && categoryId > 0 && !effectiveCategoryId.equals(categoryId)) {
            return List.of();
        }

        return subServiceService.getSubServicesByCategoryId(effectiveCategoryId);
    }

    @GetMapping("/add-main-service")
    public String showAddMainServiceForm(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("existingSubServices", List.of());
        model.addAttribute("user", provider);
        return "provider-add-main-service";
    }

    @GetMapping("/sub-services-by-category")
    @ResponseBody
    public List<SubService> getSubServicesByCategory(@RequestParam Long categoryId, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return List.of();
        }
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            return List.of();
        }
        return subServiceService.getSubServicesByCategoryId(category.getId());
    }

    @PostMapping("/add-main-service")
    public String addMainService(@RequestParam String serviceName,
                                 @RequestParam Long categoryId,
                                 @RequestParam String location,
                                 @RequestParam BigDecimal basePrice,
                                 @RequestParam(required = false) String imageUrl,
                                 @RequestParam(required = false) Long existingSubServiceId,
                                 @RequestParam(required = false) String newSubServiceName,
                                 @RequestParam(required = false) BigDecimal newSubServicePrice,
                                 HttpSession session,
                                 Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            String safeServiceName = serviceName == null ? "" : serviceName.trim();
            String safeLocation = location == null ? "" : location.trim();

            if (safeServiceName.isEmpty()) {
                throw new RuntimeException("Service name is required.");
            }
            if (safeLocation.isEmpty()) {
                throw new RuntimeException("Location is required.");
            }
            if (basePrice == null || basePrice.signum() <= 0) {
                throw new RuntimeException("Please enter a valid base price.");
            }

            Category selectedCategory = categoryService.getCategoryById(categoryId);
            if (selectedCategory == null) {
                throw new RuntimeException("Please select a valid category.");
            }

            ServiceListing mainService = new ServiceListing();
            mainService.setTitle(safeServiceName);
            mainService.setDescription("Main service listing for " + safeServiceName + ".");
            mainService.setProvider(provider);
            mainService.setCategory(selectedCategory);
            mainService.setLocation(safeLocation);
            mainService.setPrice(basePrice);
            mainService.setPriceUnit("per visit");
            mainService.setImageUrl(imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim());
            mainService.setIsAvailable(true);
            mainService.setIsCustomService(false);

            serviceService.saveService(mainService);

            SubService linkedSubService = null;
            BigDecimal linkedPrice = basePrice;

            if (existingSubServiceId != null) {
                SubService existing = subServiceService.getSubServiceById(existingSubServiceId);
                if (existing == null) {
                    throw new RuntimeException("Selected sub-service not found.");
                }
                if (existing.getCategory() == null || !selectedCategory.getId().equals(existing.getCategory().getId())) {
                    throw new RuntimeException("Selected sub-service does not belong to the chosen category.");
                }
                linkedSubService = existing;
            } else {
                String safeNewSubServiceName = newSubServiceName == null ? "" : newSubServiceName.trim();
                if (safeNewSubServiceName.isEmpty()) {
                    throw new RuntimeException("Select an existing sub-service or create a new one.");
                }

                linkedSubService = subServiceService.findByCategoryIdAndNameIgnoreCase(selectedCategory.getId(), safeNewSubServiceName)
                        .orElseGet(() -> {
                            SubService created = new SubService();
                            created.setName(safeNewSubServiceName);
                            created.setDescription(safeNewSubServiceName + " service");
                            created.setCategory(selectedCategory);
                            return subServiceService.saveSubService(created);
                        });

                if (newSubServicePrice != null && newSubServicePrice.signum() > 0) {
                    linkedPrice = newSubServicePrice;
                }
            }

            offeringService.addOrUpdateServiceOffering(
                    provider,
                    linkedSubService,
                    linkedPrice,
                    "per visit",
                    "Linked with main service: " + safeServiceName,
                    true
            );
            return "redirect:/provider/services";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("existingSubServices", categoryId != null ? subServiceService.getSubServicesByCategoryId(categoryId) : List.of());
            model.addAttribute("user", provider);
            model.addAttribute("serviceName", serviceName);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("location", location);
            model.addAttribute("basePrice", basePrice);
            model.addAttribute("imageUrl", imageUrl);
            model.addAttribute("existingSubServiceId", existingSubServiceId);
            model.addAttribute("newSubServiceName", newSubServiceName);
            model.addAttribute("newSubServicePrice", newSubServicePrice);
            return "provider-add-main-service";
        }
    }
}
