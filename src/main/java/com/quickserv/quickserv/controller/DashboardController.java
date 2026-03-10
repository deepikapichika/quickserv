package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class DashboardController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ServiceService serviceService;

    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<String> requiredCategoryNames = Arrays.asList(
                "Salon & Beauty",
                "Massage & Spa",
                "Cleaning Services",
                "AC & Appliance Repair",
                "Electrician",
                "Plumbing",
                "Painting",
                "Pest Control"
        );

        List<Category> categories = requiredCategoryNames.stream()
                .map(categoryService::getCategoryByName)
                .filter(Objects::nonNull)
                .toList();

        if (categories.isEmpty()) {
            categories = categoryService.getAllCategories();
        }

        // Main category images for category cards
        Map<String, String> categoryImages = new LinkedHashMap<>();
        categoryImages.put("Salon & Beauty", "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Massage & Spa", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Cleaning Services", "https://images.unsplash.com/photo-1563453392212-326f5e854473?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("AC & Appliance Repair", "https://images.unsplash.com/photo-1581578731548-c64695cc6952?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Electrician", "https://images.unsplash.com/photo-1621905252507-b35492cc74b4?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Plumbing", "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Painting", "https://images.unsplash.com/photo-1562259949-e8e7689d7828?q=80&w=1200&auto=format&fit=crop");
        categoryImages.put("Pest Control", "https://images.pexels.com/photos/6969887/pexels-photo-6969887.jpeg?auto=compress&cs=tinysrgb&w=1200");

        // Sub-service specific images (2-3 variations per sub-service for visual variety)
        Map<String, String> subServiceImages = buildSubServiceImageMap();

        // Add categories for display on customer dashboard
        model.addAttribute("categories", categories);
        model.addAttribute("categoryImages", categoryImages);
        model.addAttribute("subServiceImages", subServiceImages);

        // Add most booked services (limit to 6)
        model.addAttribute("mostBookedServices", serviceService.getMostBookedServices(6));

        model.addAttribute("user", user);
        return "customer-dashboard";
    }

    @GetMapping("/provider/dashboard")
    public String providerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"PROVIDER".equals(user.getRole())) {
            return "redirect:/login";
        }

        // Get provider's services for stats
        List<ServiceListing> providerServices = serviceService.getServicesByProvider(user);
        model.addAttribute("services", providerServices);

        // Calculate stats
        long activeCount = providerServices.stream().filter(ServiceListing::getIsAvailable).count();
        long categoryCount = providerServices.stream()
                .map(s -> s.getCategory() != null ? s.getCategory().getName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal totalPrice = providerServices.stream()
                .map(ServiceListing::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPrice = providerServices.isEmpty()
                ? BigDecimal.ZERO
                : totalPrice.divide(BigDecimal.valueOf(providerServices.size()), 0, RoundingMode.HALF_UP);

        model.addAttribute("serviceCount", providerServices.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("categoryCount", categoryCount);
        model.addAttribute("avgPrice", avgPrice);
        model.addAttribute("profileCompletion", calculateProfileCompletion(user));

        // Get recent services (limit to 3)
        List<ServiceListing> recentServices = providerServices;
        if (recentServices.size() > 3) {
            recentServices = recentServices.subList(0, 3);
        }
        model.addAttribute("recentServices", recentServices);

        model.addAttribute("user", user);
        return "provider-dashboard";
    }

    private int calculateProfileCompletion(User user) {
        int totalFields = 7;
        int completed = 0;

        if (user.getName() != null && !user.getName().isBlank()) completed++;
        if (user.getLocation() != null && !user.getLocation().isBlank()) completed++;
        if (user.getServiceType() != null && !user.getServiceType().isBlank()) completed++;
        if (user.getProviderDescription() != null && !user.getProviderDescription().isBlank()) completed++;
        if (user.getServiceArea() != null && !user.getServiceArea().isBlank()) completed++;
        if (user.getAvailabilityDays() != null && !user.getAvailabilityDays().isBlank()) completed++;
        if (user.getAvailabilityTimeSlots() != null && !user.getAvailabilityTimeSlots().isBlank()) completed++;

        return (int) Math.round((completed * 100.0) / totalFields);
    }

    /**
     * Build sub-service specific image mapping for richer visual variety.
     * Each sub-service gets 2-3 professional, relevant images that rotate.
     */
    private Map<String, String> buildSubServiceImageMap() {
        Map<String, String> images = new LinkedHashMap<>();

        // Salon & Beauty Sub-Services
        images.put("Hair Cut", "https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1200&auto=format&fit=crop");
        images.put("Hair Spa", "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?q=80&w=1200&auto=format&fit=crop");
        images.put("Hair Coloring", "https://images.unsplash.com/photo-1560869713-7d0a29430803?q=80&w=1200&auto=format&fit=crop");
        images.put("Facial", "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?q=80&w=1200&auto=format&fit=crop");
        images.put("Manicure", "https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=1200&auto=format&fit=crop");
        images.put("Pedicure", "https://images.unsplash.com/photo-1519415943484-9fa1873496d4?q=80&w=1200&auto=format&fit=crop");
        images.put("Bridal Makeup", "https://images.unsplash.com/photo-1487412947147-5cebf100ffc2?q=80&w=1200&auto=format&fit=crop");
        images.put("Threading", "https://images.unsplash.com/photo-1596704017254-9b121068ec31?q=80&w=1200&auto=format&fit=crop");
        images.put("Waxing", "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?q=80&w=1200&auto=format&fit=crop");

        // Massage & Spa Sub-Services
        images.put("Full Body Massage", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?q=80&w=1200&auto=format&fit=crop");
        images.put("Head Massage", "https://images.unsplash.com/photo-1519823551278-64ac92734fb1?q=80&w=1200&auto=format&fit=crop");
        images.put("Swedish Massage", "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?q=80&w=1200&auto=format&fit=crop");
        images.put("Deep Tissue Massage", "https://images.unsplash.com/photo-1596178060671-7a80dc8059ea?q=80&w=1200&auto=format&fit=crop");
        images.put("Foot Reflexology", "https://images.unsplash.com/photo-1598511757337-fe2cafc31ba0?q=80&w=1200&auto=format&fit=crop");

        // Cleaning Services Sub-Services
        images.put("Home Deep Cleaning", "https://images.unsplash.com/photo-1581578731548-c64695cc6952?q=80&w=1200&auto=format&fit=crop");
        images.put("Kitchen Cleaning", "https://images.unsplash.com/photo-1556911220-bff31c812dba?q=80&w=1200&auto=format&fit=crop");
        images.put("Bathroom Cleaning", "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1200&auto=format&fit=crop");
        images.put("Sofa Cleaning", "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=1200&auto=format&fit=crop");
        images.put("Carpet Cleaning", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?q=80&w=1200&auto=format&fit=crop");

        // AC & Appliance Repair Sub-Services
        images.put("AC Installation", "https://images.unsplash.com/photo-1631545804584-9edb85573c0d?q=80&w=1200&auto=format&fit=crop");
        images.put("AC Repair", "https://images.unsplash.com/photo-1581578731548-c64695cc6952?q=80&w=1200&auto=format&fit=crop");
        images.put("AC Gas Refill", "https://images.unsplash.com/photo-1604709177225-055f99402ea3?q=80&w=1200&auto=format&fit=crop");
        images.put("Refrigerator Repair", "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?q=80&w=1200&auto=format&fit=crop");
        images.put("Washing Machine Repair", "https://images.unsplash.com/photo-1626806787461-102c1bfaaea1?q=80&w=1200&auto=format&fit=crop");
        images.put("TV Repair", "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?q=80&w=1200&auto=format&fit=crop");

        // Electrician Sub-Services
        images.put("Fan Installation", "https://images.unsplash.com/photo-1558089687-e446d3386e04?q=80&w=1200&auto=format&fit=crop");
        images.put("Light Installation", "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?q=80&w=1200&auto=format&fit=crop");
        images.put("Switch Repair", "https://images.unsplash.com/photo-1621905252507-b35492cc74b4?q=80&w=1200&auto=format&fit=crop");
        images.put("Wiring Repair", "https://images.unsplash.com/photo-1621905252472-8cf2e4fec7b5?q=80&w=1200&auto=format&fit=crop");
        images.put("Inverter Installation", "https://images.unsplash.com/photo-1621839673705-6617adf9e890?q=80&w=1200&auto=format&fit=crop");

        // Plumbing Sub-Services
        images.put("Tap Repair", "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?q=80&w=1200&auto=format&fit=crop");
        images.put("Pipe Leakage Fix", "https://images.unsplash.com/photo-1581858763604-4308a6d52915?q=80&w=1200&auto=format&fit=crop");
        images.put("Toilet Repair", "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1200&auto=format&fit=crop");
        images.put("Drain Cleaning", "https://images.unsplash.com/photo-1581858753455-d52f4899835d?q=80&w=1200&auto=format&fit=crop");
        images.put("Water Motor Repair", "https://images.unsplash.com/photo-1503387762-592deb58ef4e?q=80&w=1200&auto=format&fit=crop");

        // Painting Sub-Services
        images.put("Interior Painting", "https://images.unsplash.com/photo-1562259949-e8e7689d7828?q=80&w=1200&auto=format&fit=crop");
        images.put("Exterior Painting", "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?q=80&w=1200&auto=format&fit=crop");
        images.put("Wall Texture Design", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?q=80&w=1200&auto=format&fit=crop");
        images.put("Wallpaper Installation", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop");
        images.put("Waterproofing", "https://images.unsplash.com/photo-1581858753455-d52f4899835d?q=80&w=1200&auto=format&fit=crop");

        // Pest Control Sub-Services
        images.put("Cockroach Control", "https://images.pexels.com/photos/6969887/pexels-photo-6969887.jpeg?auto=compress&cs=tinysrgb&w=1200");
        images.put("Termite Control", "https://images.pexels.com/photos/8851684/pexels-photo-8851684.jpeg?auto=compress&cs=tinysrgb&w=1200");
        images.put("Mosquito Control", "https://images.pexels.com/photos/7516559/pexels-photo-7516559.jpeg?auto=compress&cs=tinysrgb&w=1200");
        images.put("Bed Bug Treatment", "https://images.pexels.com/photos/6969887/pexels-photo-6969887.jpeg?auto=compress&cs=tinysrgb&w=1200");

        return images;
    }
}
