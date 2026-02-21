package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository  // Tells Spring this is a repository component
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring Data JPA automatically provides:
    // - save()         → Save a category
    // - findAll()      → Get all categories
    // - findById()     → Get one category by ID
    // - delete()       → Delete a category
    // - count()        → Count total categories

    // Custom method: Find a category by its name
    // Spring automatically creates the SQL: SELECT * FROM categories WHERE name = ?
    Category findByName(String name);
}