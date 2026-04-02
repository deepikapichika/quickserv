package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderService;
import com.quickserv.quickserv.service.ServiceService;
import com.quickserv.quickserv.service.UserService;
import com.quickserv.quickserv.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired private CategoryService categoryService;
    @Autowired private ServiceService serviceService;
    @Autowired private BookingService bookingService;
    @Autowired private UserService userService;
    @Autowired private ProviderService providerService;
    @Autowired private ReviewService reviewService;

    @GetMapping("/dashboard")
    public String redirectDashboard(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if ("ADMIN".equals(user.getRole())) return "redirect:/admin/dashboard";
        if ("PROVIDER".equals(user.getRole())) return "redirect:/provider/dashboard";
        return "redirect:/customer/dashboard";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (!"CUSTOMER".equals(user.getRole())) return "redirect:/dashboard";

        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        List<ServiceListing> recentServices = serviceService.getAvailableServices();
        if (recentServices.size() > 4) recentServices = recentServices.subList(0, 4);
        model.addAttribute("recentServices", recentServices);
        model.addAttribute("user", user);
        return "customer-dashboard";
    }

    @GetMapping("/provider/dashboard")
    public String providerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (!"PROVIDER".equals(user.getRole())) return "redirect:/dashboard";

        List<ServiceListing> providerServices = serviceService.getServicesByProvider(user);
        model.addAttribute("services", providerServices);

        long activeCount = providerServices.stream().filter(ServiceListing::getIsAvailable).count();
        model.addAttribute("serviceCount", providerServices.size());
        model.addAttribute("activeCount", activeCount);

        List<ServiceListing> recentServices = providerServices.size() > 3
                ? providerServices.subList(0, 3) : providerServices;
        model.addAttribute("recentServices", recentServices);

        BookingService.BookingStats bookingStats = bookingService.getBookingStats(user);
        model.addAttribute("bookingStats", bookingStats);
        model.addAttribute("providerProfile", providerService.getProviderByUser(user));
        model.addAttribute("user", user);
        return "provider-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        if (!"ADMIN".equals(user.getRole())) return "redirect:/dashboard";

        List<User> allUsers = userService.getAllUsers();
        List<Provider> allProviders = providerService.getAllProviders();
        List<Booking> allBookings = bookingService.getAllBookings();
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("user", user);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("allProviders", allProviders);
        model.addAttribute("allBookings", allBookings);
        model.addAttribute("categories", categories);
        model.addAttribute("userCount", allUsers.size());
        model.addAttribute("providerCount", allProviders.size());
        model.addAttribute("bookingCount", allBookings.size());
        model.addAttribute("categoryCount", categoryService.countCategories());
        return "admin-dashboard";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || !"ADMIN".equals(admin.getRole())) return "redirect:/login";

        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own admin account.");
            return "redirect:/admin/dashboard";
        }

        try {
            userService.deleteUserById(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    /**
     * Display customer's bookings (My Bookings page)
     */
    @GetMapping("/customer/bookings")
    public String getMyBookings(HttpSession session, Model model) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null) {
            return "redirect:/login";
        }
        if (!"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/dashboard";
        }

        List<Booking> bookings = bookingService.getCustomerBookings(customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("reviewedBookingIds", reviewService.getReviewedBookingIdsForCustomer(customer.getId()));
        model.addAttribute("user", customer);
        return "customer-bookings";
    }
}