package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;  // Add this line
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            userService.registerUser(user);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "hello";  // Back to registration form with error
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
            if ("ADMIN".equals(user.getRole())) {
                System.out.println("Redirecting to ADMIN dashboard");
                return "redirect:/admin/dashboard";
            } else if ("PROVIDER".equals(user.getRole())) {
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
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Clear all session data
        return "redirect:/login?logout=true";  // Redirect to login with message
    }
}