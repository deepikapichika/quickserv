package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceListing, Long> {

    // Find all services by a specific provider
    // SQL: SELECT * FROM services WHERE provider_id = ?
    List<ServiceListing> findByProvider(User provider);

    // Find all services in a specific category
    // SQL: SELECT * FROM services WHERE category_id = ?
    List<ServiceListing> findByCategory(Category category);

    // Find only available services
    // SQL: SELECT * FROM services WHERE is_available = true
    List<ServiceListing> findByIsAvailableTrue();

    // Custom search using JPQL (Java Persistence Query Language)
    // This searches in title OR description for the keyword
    @Query("SELECT s FROM ServiceListing s WHERE " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ServiceListing> searchServices(@Param("keyword") String keyword);
}
