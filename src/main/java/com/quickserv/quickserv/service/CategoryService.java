package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service  // Tells Spring this is a Service component
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Get all categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Get one category by ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    // Get category by name
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    // Save a new category
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Delete a category
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
