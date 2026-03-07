package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ServiceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookingService bookingService;

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
        model.addAttribute("categories", categoryService.getAllCategories());
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
                                 @RequestParam(required = false) String search,
                                 Model model,
                                 HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<ServiceListing> services;

        if (categoryId != null && categoryId > 0) {
            Category category = categoryService.getCategoryById(categoryId);
            services = serviceService.getServicesByCategory(category);
        } else if (search != null && !search.isEmpty()) {
            services = serviceService.searchServices(search);
        } else {
            services = serviceService.getAvailableServices();
        }

        model.addAttribute("services", services);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("user", user);

        return "browse-services";
    }

    // View single service details
    @GetMapping("/service/{id}")
    public String viewService(@PathVariable Long id,
                              Model model,
                              HttpSession session) {

        System.out.println("=== VIEW SERVICE CALLED ===");
        System.out.println("Service ID: " + id);

        User user = (User) session.getAttribute("loggedInUser");
        System.out.println("User: " + (user != null ? user.getEmail() : "null"));

        if (user == null) {
            System.out.println("No user - redirecting to login");
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        System.out.println("Service found: " + (service != null ? service.getTitle() : "null"));

        if (service == null) {
            System.out.println("Service not found - redirecting to browse");
            return "redirect:/browse";
        }

        model.addAttribute("service", service);
        model.addAttribute("user", user);
        return "service-detail";
    }

    // ============= BOOKING SECTION =============

    // Create a booking (Customer)
    @PostMapping("/service/{id}/book")
    public String createBooking(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                               @RequestParam(required = false) String notes,
                               HttpSession session,
                               Model model) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(id);
        if (service == null || !service.getIsAvailable()) {
            model.addAttribute("error", "Service not available");
            return "redirect:/service/" + id;
        }

        try {
            Booking booking = bookingService.createBooking(customer, service, bookingDateTime, notes);
            model.addAttribute("success", "Booking created successfully!");
            return "redirect:/customer/bookings";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/service/" + id;
        }
    }

    // Show provider bookings
    @GetMapping("/provider/bookings")
    public String showProviderBookings(HttpSession session, Model model) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getProviderBookings(provider);
        BookingService.BookingStats stats = bookingService.getBookingStats(provider);

        model.addAttribute("bookings", bookings);
        model.addAttribute("stats", stats);
        return "provider-bookings";
    }

    // Update booking status (Provider)
    @PostMapping("/provider/booking/{id}/status")
    public String updateBookingStatus(@PathVariable Long id,
                                     @RequestParam Booking.BookingStatus status,
                                     HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.updateBookingStatus(id, status, provider);
        } catch (RuntimeException e) {
            // Handle error
        }

        return "redirect:/provider/bookings";
    }

    // Add provider notes to booking
    @PostMapping("/provider/booking/{id}/notes")
    public String addProviderNotes(@PathVariable Long id,
                                  @RequestParam String notes,
                                  HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.addProviderNotes(id, notes, provider);
        } catch (RuntimeException e) {
            // Handle error
        }

        return "redirect:/provider/bookings";
    }
}
