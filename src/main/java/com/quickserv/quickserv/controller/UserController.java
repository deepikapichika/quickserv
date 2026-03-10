package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());  // Empty user object for form binding
        model.addAttribute("categories", categoryService.getProviderCategoryOptions()); // Add categories to model
        return "register";  // This looks for register.html
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            userService.registerUser(user);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user); // Preserve user data on error
            model.addAttribute("categories", categoryService.getProviderCategoryOptions()); // Add categories to model
            return "register";  // Back to registration form with error
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               Model model,
                               HttpSession session) {

        boolean isValid = userService.login(email, password);

        if (isValid) {
            User user = userService.findByEmail(email);
            session.setAttribute("loggedInUser", user);

            // DEBUG: Print role to console
            System.out.println("USER ROLE: " + user.getRole());
            System.out.println("USER NAME: " + user.getName());

            // Redirect based on role
            if ("PROVIDER".equals(user.getRole())) {
                System.out.println("Redirecting to PROVIDER dashboard");
                return "redirect:/provider/dashboard";
            } else {
                System.out.println("Redirecting to CUSTOMER dashboard");
                return "redirect:/customer/dashboard";
            }
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        try {
            userService.generateAndSendPasswordResetToken(email);
            model.addAttribute("message", "If this email is registered, a password reset link has been sent.");
        } catch (RuntimeException e) {
            model.addAttribute("error", "Unable to process your request right now. Please try again.");
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        if (!userService.isResetTokenValid(token)) {
            model.addAttribute("error", "Reset link is invalid or expired.");
            return "forgot-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        try {
            userService.resetPassword(token, password);
            return "redirect:/login?resetSuccess=true";
        } catch (RuntimeException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }

    @GetMapping("/provider/profile")
    public String showProviderProfile(HttpSession session,
                                      Model model,
                                      @RequestParam(required = false) String success) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"PROVIDER".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        User provider = userService.findById(loggedInUser.getId());
        model.addAttribute("provider", provider);
        model.addAttribute("categories", categoryService.getProviderCategoryOptions());

        if (success != null) {
            model.addAttribute("success", "Profile updated successfully.");
        }

        return "provider-profile";
    }

    @PostMapping("/provider/profile")
    public String updateProviderProfile(@ModelAttribute("provider") User providerInput,
                                        HttpSession session,
                                        Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"PROVIDER".equals(loggedInUser.getRole())) {
            return "redirect:/login";
        }

        try {
            User updatedUser = userService.updateProviderProfile(loggedInUser.getId(), providerInput);
            session.setAttribute("loggedInUser", updatedUser);
            return "redirect:/provider/profile?success=true";
        } catch (RuntimeException e) {
            User provider = userService.findById(loggedInUser.getId());
            if (provider != null) {
                provider.setName(providerInput.getName());
                provider.setLocation(providerInput.getLocation());
                provider.setServiceType(providerInput.getServiceType());
                provider.setProfilePhotoUrl(providerInput.getProfilePhotoUrl());
                provider.setProviderDescription(providerInput.getProviderDescription());
                provider.setExperienceYears(providerInput.getExperienceYears());
                provider.setServiceArea(providerInput.getServiceArea());
                provider.setAvailabilityDays(providerInput.getAvailabilityDays());
                provider.setAvailabilityTimeSlots(providerInput.getAvailabilityTimeSlots());
            }
            model.addAttribute("provider", provider);
            model.addAttribute("categories", categoryService.getProviderCategoryOptions());
            model.addAttribute("error", e.getMessage());
            return "provider-profile";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Clear all session data
        return "redirect:/login?logout=true";  // Redirect to login with message
    }
}