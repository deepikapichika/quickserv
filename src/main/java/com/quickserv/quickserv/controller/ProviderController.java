package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.dto.provider.ProviderCreateRequest;
import com.quickserv.quickserv.dto.provider.ProviderResponse;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/provider")
public class ProviderController {

    private final ProviderService providerService;
    private final CategoryService categoryService;

    public ProviderController(ProviderService providerService, CategoryService categoryService) {
        this.providerService = providerService;
        this.categoryService = categoryService;
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addProvider(@Valid @RequestBody ProviderCreateRequest request,
                                                           HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Please log in to continue."
            ));
        }

        // Non-admin users can only create/update provider profile for their own account.
        if (!"ADMIN".equalsIgnoreCase(loggedInUser.getRole()) && !loggedInUser.getId().equals(request.getUserId())) {
            throw new BusinessValidationException("Users can only register provider details for their own account.");
        }

        ProviderResponse provider = providerService.addProvider(request);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("message", "Provider registered successfully.");
        payload.put("provider", provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

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

    @GetMapping("/byCategory/{categoryId}")
    @ResponseBody
    public List<ProviderResponse> getProvidersByCategory(@PathVariable Long categoryId) {
        return providerService.getProviderResponsesByCategory(categoryId);
    }

    @GetMapping("/byLocation/{location}")
    @ResponseBody
    public List<ProviderResponse> getProvidersByLocation(@PathVariable String location) {
        return providerService.getProviderResponsesByLocation(location);
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
