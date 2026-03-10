package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.SubService;
import com.quickserv.quickserv.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubServiceRepository extends JpaRepository<SubService, Long> {

    // Find all sub-services for a specific category
    List<SubService> findByCategory(Category category);

    // Find all sub-services by category ID
    List<SubService> findByCategoryId(Long categoryId);

    Optional<SubService> findByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}
