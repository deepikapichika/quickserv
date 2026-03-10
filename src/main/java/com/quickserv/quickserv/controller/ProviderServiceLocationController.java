package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.ProviderServiceLocation;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.ProviderServiceLocationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/provider/locations")
public class ProviderServiceLocationController {

    @Autowired
    private ProviderServiceLocationService locationService;

    @GetMapping
    public String listLocations(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<ProviderServiceLocation> locations = locationService.getProviderLocations(provider);
        ProviderServiceLocation primaryLocation = locationService.getPrimaryLocation(provider);

        model.addAttribute("locations", locations);
        model.addAttribute("primaryLocation", primaryLocation);
        model.addAttribute("user", provider);

        return "provider-service-locations";
    }

    @PostMapping("/add")
    public String addLocation(
            @RequestParam String locationName,
            @RequestParam(required = false) String address,
            HttpSession session,
            Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            locationService.addLocation(provider, locationName, address);
            return "redirect:/provider/locations?added=true";
        } catch (RuntimeException e) {
            List<ProviderServiceLocation> locations = locationService.getProviderLocations(provider);
            model.addAttribute("locations", locations);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", provider);

            return "provider-service-locations";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateLocation(
            @PathVariable Long id,
            @RequestParam String locationName,
            @RequestParam(required = false) String address,
            @RequestParam Boolean isActive,
            HttpSession session,
            Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            ProviderServiceLocation location = locationService.getLocationById(id);
            if (location == null || !location.getProvider().getId().equals(provider.getId())) {
                return "redirect:/provider/locations";
            }

            locationService.updateLocation(id, locationName, address, isActive);
            return "redirect:/provider/locations?updated=true";
        } catch (RuntimeException e) {
            List<ProviderServiceLocation> locations = locationService.getProviderLocations(provider);
            model.addAttribute("locations", locations);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", provider);

            return "provider-service-locations";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteLocation(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        ProviderServiceLocation location = locationService.getLocationById(id);
        if (location != null && location.getProvider().getId().equals(provider.getId())) {
            locationService.deleteLocation(id);
        }

        return "redirect:/provider/locations?deleted=true";
    }

    @PostMapping("/set-primary/{id}")
    public String setPrimaryLocation(@PathVariable Long id, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        locationService.setPrimaryLocation(provider, id);
        return "redirect:/provider/locations?primary=true";
    }
}

