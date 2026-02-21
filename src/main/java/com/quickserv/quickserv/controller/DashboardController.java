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
import java.util.List;

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

        // Add categories for display on customer dashboard
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Add recent services (limit to 4)
        List<ServiceListing> recentServices = serviceService.getAvailableServices();
        if (recentServices.size() > 4) {
            recentServices = recentServices.subList(0, 4);
        }
        model.addAttribute("recentServices", recentServices);

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
        model.addAttribute("serviceCount", providerServices.size());
        model.addAttribute("activeCount", activeCount);

        // Get recent services (limit to 3)
        List<ServiceListing> recentServices = providerServices;
        if (recentServices.size() > 3) {
            recentServices = recentServices.subList(0, 3);
        }
        model.addAttribute("recentServices", recentServices);

        model.addAttribute("user", user);
        return "provider-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "admin-dashboard";
    }
}