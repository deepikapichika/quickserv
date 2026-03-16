package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/category/all")
    public List<Map<String, Object>> getAllCategories() {
        return categoryService.getAllCategories().stream().map(this::toCategoryPayload).toList();
    }

    @PostMapping("/category/add")
    public ResponseEntity<?> addCategory(@RequestParam(name = "category_name", required = false) String categoryName,
                                         @RequestParam(name = "categoryName", required = false) String categoryNameAlt,
                                         @RequestParam(required = false) String description,
                                         HttpSession session) {
        ResponseEntity<?> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        try {
            Category saved = categoryService.createCategory(resolveCategoryName(categoryName, categoryNameAlt), description);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Category added successfully.",
                    "category", toCategoryPayload(saved)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, HttpSession session) {
        ResponseEntity<?> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Category deleted successfully."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private ResponseEntity<?> requireAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Please log in to continue."
            ));
        }
        if (!"ADMIN".equals(loggedInUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Only ADMIN users can manage categories."
            ));
        }
        return null;
    }

    private String resolveCategoryName(String categoryName, String categoryNameAlt) {
        return categoryName != null && !categoryName.isBlank() ? categoryName : categoryNameAlt;
    }

    private Map<String, Object> toCategoryPayload(Category category) {
        return Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() == null ? "" : category.getDescription()
        );
    }
}

