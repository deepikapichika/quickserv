package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findOptionalByUser(User user);

    Provider findByUser(User user);

    boolean existsByUser(User user);

    // Keep compatibility with CategoryService while checking direct and join-table mappings.
    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Provider p
            LEFT JOIN p.selectedCategories sc
            WHERE p.category = :category OR sc = :category
            """)
    boolean existsByCategory(@Param("category") Category category);

    @Query("""
            SELECT DISTINCT p
            FROM Provider p
            LEFT JOIN p.selectedCategories sc
            WHERE p.category.id = :categoryId OR sc.id = :categoryId
            """)
    List<Provider> findProvidersByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT p
            FROM Provider p
            JOIN p.user u
            WHERE LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%'))
            """)
    List<Provider> findByLocation(@Param("location") String location);

    @Query("""
            SELECT DISTINCT p
            FROM Provider p
            LEFT JOIN p.selectedCategories sc
            JOIN p.user u
            WHERE (p.category.id = :categoryId OR sc.id = :categoryId)
              AND LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%'))
            """)
    List<Provider> findByCategoryAndLocation(@Param("categoryId") Long categoryId,
                                             @Param("location") String location);

    List<Provider> findTop10ByOrderByRatingDesc();

    List<Provider> findByRatingGreaterThanEqual(Double rating);
}
