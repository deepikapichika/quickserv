package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderService;
import com.quickserv.quickserv.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProviderService providerService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "register";
    }

    @PostMapping(value = "/register", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Object registerUser(@ModelAttribute User user,
                               @RequestParam(name = "category_id", required = false) Long categoryId,
                               @RequestParam(name = "categoryId", required = false) Long categoryIdAlt,
                               @RequestParam(required = false) List<Long> categoryIds,
                               @RequestParam(required = false) String categoryIdsCsv,
                               @RequestParam(required = false) List<Long> serviceIds,
                               @RequestParam(required = false) String serviceIdsCsv,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                               HttpSession session,
                               Model model) {
        try {
            Long resolvedCategoryId = resolveCategoryId(categoryId, categoryIdAlt, categoryIds, categoryIdsCsv);
            List<Long> resolvedServiceIds = resolveIds(serviceIds, serviceIdsCsv);

            User savedUser = userService.registerUser(user);

            if ("PROVIDER".equals(savedUser.getRole())) {
                providerService.registerProviderFromRegistration(savedUser, resolvedCategoryId, resolvedServiceIds, categoryService);
            }

            session.setAttribute("loggedInUser", savedUser);
            String redirectPath = redirectByRole(savedUser).replace("redirect:", "");

            if (isAjaxRequest(requestedWith)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Registration successful",
                        "redirect", redirectPath
                ));
            }

            return redirectByRole(savedUser);
        } catch (RuntimeException e) {
            if (isAjaxRequest(requestedWith)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("user", user);
            return "register";
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
            } else {
                System.out.println("Redirecting to shared dashboard");
                return "redirect:/dashboard"; // Shared dashboard for PROVIDER and CUSTOMER
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

    private String redirectByRole(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }
        if ("PROVIDER".equals(user.getRole())) {
            return "redirect:/provider/dashboard";
        }
        return "redirect:/customer/dashboard";
    }

    private boolean isAjaxRequest(String requestedWith) {
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private Long resolveCategoryId(Long categoryId,
                                   Long categoryIdAlt,
                                   List<Long> categoryIds,
                                   String categoryIdsCsv) {
        if (categoryId != null) {
            return categoryId;
        }
        if (categoryIdAlt != null) {
            return categoryIdAlt;
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            return categoryIds.get(0);
        }
        if (categoryIdsCsv != null && !categoryIdsCsv.trim().isEmpty()) {
            String first = categoryIdsCsv.split(",")[0].trim();
            if (!first.isEmpty()) {
                return Long.parseLong(first);
            }
        }
        return null;
    }

    private List<Long> resolveIds(List<Long> ids, String idsCsv) {
        List<Long> resolved = new ArrayList<>();
        if (ids != null) {
            resolved.addAll(ids.stream().filter(id -> id != null && id > 0).toList());
        }

        if (idsCsv != null && !idsCsv.trim().isEmpty()) {
            String[] parts = idsCsv.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                resolved.add(Long.parseLong(trimmed));
            }
        }

        return resolved.stream().distinct().toList();
    }
}
