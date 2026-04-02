package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews/add")
    public String addReview(@RequestParam Long bookingId,
                            @RequestParam Integer rating,
                            @RequestParam(required = false) String comment,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User customer = (User) session.getAttribute("loggedInUser");
        if (customer == null || !"CUSTOMER".equalsIgnoreCase(customer.getRole())) {
            return "redirect:/login";
        }

        try {
            reviewService.createReview(bookingId, customer, rating, comment);
            redirectAttributes.addFlashAttribute("reviewSuccessMessage", "Thank you! Your review was submitted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", ex.getMessage());
        }

        return "redirect:/customer/bookings";
    }
}

