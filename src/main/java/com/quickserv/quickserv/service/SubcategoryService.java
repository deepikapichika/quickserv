package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Subcategory;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.SubcategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;

    public SubcategoryService(SubcategoryRepository subcategoryRepository) {
        this.subcategoryRepository = subcategoryRepository;
    }

    public List<Subcategory> getAllSubcategories() {
        return subcategoryRepository.findAllByOrderByNameAsc();
    }

    public List<Subcategory> getByCategory(Long categoryId) {
        return subcategoryRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }

    public Subcategory getById(Long id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found for id: " + id));
    }
}

