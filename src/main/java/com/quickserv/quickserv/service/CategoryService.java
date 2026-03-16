package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProviderRepository providerRepository;

    // Get all categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    // Get one category by ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    // Get category by name
    public Category getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return categoryRepository.findByNameIgnoreCase(name.trim()).orElse(null);
    }

    // Save a new category
    public Category saveCategory(Category category) {
        return createCategory(category.getName(), category.getDescription());
    }

    public Category createCategory(String categoryName, String description) {
        String cleanName = normalizeRequiredName(categoryName);

        if (categoryRepository.existsByNameIgnoreCase(cleanName)) {
            throw new RuntimeException("Category name must be unique.");
        }

        Category category = new Category();
        category.setName(cleanName);
        category.setDescription(normalizeDescription(description));
        return categoryRepository.save(category);
    }

    // Delete a category
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found."));

        if (providerRepository.existsByCategory(category)) {
            throw new RuntimeException("Cannot delete category because it is currently assigned to providers.");
        }

        categoryRepository.delete(category);
    }

    public long countCategories() {
        return categoryRepository.count();
    }

    private String normalizeRequiredName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new RuntimeException("Category name must not be empty.");
        }
        return categoryName.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        return description.trim();
    }
}
