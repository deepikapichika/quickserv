package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
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
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ServiceService serviceService;

    // Customer: View booking form
    @GetMapping("/new/{serviceId}")
    public String showBookingForm(@PathVariable Long serviceId, Model model, HttpSession session) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null) {
            return "redirect:/login";
        }

        ServiceListing service = serviceService.getServiceById(serviceId);
        if (service == null) {
            return "redirect:/browse";
        }

        model.addAttribute("service", service);
        model.addAttribute("customer", customer);
        return "booking-form";
    }

    // Customer: Create booking
    @PostMapping("/create")
    public String createBooking(@RequestParam Long serviceId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bookingDateTime,
                                @RequestParam(required = false) String customerNotes,
                                HttpSession session,
                                Model model) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/login";
        }

        try {
            ServiceListing service = serviceService.getServiceById(serviceId);
            if (service == null || !service.getIsAvailable()) {
                model.addAttribute("error", "Service not available");
                return "redirect:/service/" + serviceId;
            }

            Booking booking = bookingService.createBooking(customer, service, bookingDateTime, customerNotes);
            model.addAttribute("message", "Booking created successfully!");
            model.addAttribute("bookingId", booking.getId());
            return "redirect:/bookings/customer";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/service/" + serviceId;
        }
    }

    // Customer: View my bookings
    @GetMapping("/customer")
    public String customerBookings(Model model, HttpSession session) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getCustomerBookings(customer);
        model.addAttribute("bookings", bookings);
        model.addAttribute("user", customer);
        return "customer-bookings";
    }

    // Customer: Cancel booking
    @PostMapping("/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId, HttpSession session) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(bookingId, customer);
        } catch (RuntimeException e) {
            // Log error and continue
            System.out.println("Error canceling booking: " + e.getMessage());
        }

        return "redirect:/bookings/customer";
    }

    // Provider: View bookings
    @GetMapping("/provider")
    public String providerBookings(Model model, HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getProviderBookings(provider);
        BookingService.BookingStats stats = bookingService.getBookingStats(provider);

        model.addAttribute("bookings", bookings);
        model.addAttribute("stats", stats);
        model.addAttribute("user", provider);
        return "provider-bookings";
    }

    // Provider: Update booking status
    @PostMapping("/provider/update-status")
    public String updateBookingStatus(@RequestParam Long bookingId,
                                      @RequestParam Booking.BookingStatus status,
                                      @RequestParam(required = false) String providerNotes,
                                      HttpSession session) {
        User provider = (User) session.getAttribute("loggedInUser");
        if (provider == null || !"PROVIDER".equals(provider.getRole())) {
            return "redirect:/login";
        }

        try {
            bookingService.updateBookingStatus(bookingId, status, provider, providerNotes);
        } catch (RuntimeException e) {
            System.out.println("Error updating booking status: " + e.getMessage());
        }

        return "redirect:/bookings/provider";
    }

    // View single booking details
    @GetMapping("/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId, Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingById(bookingId);

            // Check if user has access to this booking
            if (!booking.getCustomer().getId().equals(currentUser.getId()) &&
                !booking.getProvider().getId().equals(currentUser.getId())) {
                return "redirect:/customer/dashboard";
            }

            model.addAttribute("booking", booking);
            model.addAttribute("user", currentUser);
            return "booking-detail";
        } catch (RuntimeException e) {
            return "redirect:/customer/dashboard";
        }
    }
}

