package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    // Custom method: Find a category by its name, case-insensitively
    // Spring automatically creates the SQL: SELECT * FROM categories WHERE LOWER(name) = LOWER(?)
    Optional<Category> findByNameIgnoreCase(String name);

    // Custom method: Check if a category exists by its name, case-insensitively
    // Spring automatically creates the SQL: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM categories WHERE LOWER(name) = LOWER(?)
    boolean existsByNameIgnoreCase(String name);

    // Custom method: Get all categories sorted by name in ascending order
    // Spring automatically creates the SQL: SELECT * FROM categories ORDER BY name ASC
    List<Category> findAllByOrderByNameAsc();
}