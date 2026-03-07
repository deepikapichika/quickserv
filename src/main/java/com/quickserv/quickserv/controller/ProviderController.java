package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.ProviderService;
import com.quickserv.quickserv.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/provider")
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private CategoryService categoryService;

    // Register as provider (for users who want to become providers)
    @PostMapping("/register")
    public String registerAsProvider(@RequestParam Long categoryId,
                                   @RequestParam BigDecimal serviceCharge,
                                   @RequestParam(required = false) String experience,
                                   @RequestParam(required = false) String availability,
                                   HttpSession session,
                                   Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Provider provider = providerService.registerProvider(user, categoryId, serviceCharge, categoryService);
            if (experience != null) provider.setExperience(experience);
            if (availability != null) provider.setAvailability(availability);
            providerService.updateProviderProfile(provider.getProviderId(), experience, availability, serviceCharge);

            model.addAttribute("success", "Successfully registered as a service provider!");
            return "redirect:/provider/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    // Get providers by category
    @GetMapping("/byCategory/{categoryId}")
    @ResponseBody
    public List<Provider> getProvidersByCategory(@PathVariable Long categoryId) {
        return providerService.getProvidersByCategory(categoryId);
    }

    // Get providers by location
    @GetMapping("/byLocation/{location}")
    @ResponseBody
    public List<Provider> getProvidersByLocation(@PathVariable String location) {
        return providerService.getProvidersByLocation(location);
    }

    // Provider profile page
    @GetMapping("/profile/{id}")
    public String viewProviderProfile(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Provider provider = providerService.getProviderById(id);
        if (provider == null) {
            return "redirect:/browse";
        }

        model.addAttribute("provider", provider);
        model.addAttribute("user", user);
        return "provider-profile";
    }

    // Update provider profile
    @PostMapping("/profile/update")
    public String updateProviderProfile(@RequestParam Long providerId,
                                      @RequestParam(required = false) String experience,
                                      @RequestParam(required = false) String availability,
                                      @RequestParam(required = false) BigDecimal serviceCharge,
                                      HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"PROVIDER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Provider provider = providerService.getProviderByUser(user);
        if (provider == null || !provider.getProviderId().equals(providerId)) {
            return "redirect:/provider/dashboard";
        }

        providerService.updateProviderProfile(providerId, experience, availability, serviceCharge);
        return "redirect:/provider/profile/" + providerId;
    }
}
