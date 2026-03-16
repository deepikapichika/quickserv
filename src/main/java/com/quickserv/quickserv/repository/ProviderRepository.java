package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    // Find provider by user
    Provider findByUser(User user);

    // Find providers by category
    List<Provider> findByCategory(Category category);

    boolean existsByCategory(Category category);

    // Find providers by category ID
    List<Provider> findByCategoryId(Long categoryId);

    // Find providers by location (through user)
    @Query("SELECT p FROM Provider p WHERE p.user.location LIKE %:location%")
    List<Provider> findByLocation(@Param("location") String location);

    // Find providers by category and location
    @Query("SELECT p FROM Provider p WHERE p.category.id = :categoryId AND p.user.location LIKE %:location%")
    List<Provider> findByCategoryAndLocation(@Param("categoryId") Long categoryId, @Param("location") String location);

    // Find top rated providers
    List<Provider> findTop10ByOrderByRatingDesc();

    // Find providers with rating above threshold
    List<Provider> findByRatingGreaterThanEqual(Double rating);
}
