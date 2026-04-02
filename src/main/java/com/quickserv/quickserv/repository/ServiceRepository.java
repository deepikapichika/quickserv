package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.dto.search.ServiceSearchResultDto;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    List<ServiceListing> findByProviderAndCategory(User provider, Category category);

    boolean existsByProviderAndCategoryAndTitleIgnoreCase(User provider, Category category, String title);

    @Query("""
            SELECT s
            FROM ServiceListing s
            WHERE s.category.id = :categoryId
            ORDER BY s.title ASC
            """)
    List<ServiceListing> findForCategoryLookup(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT s
            FROM ServiceListing s
            WHERE s.category.id = :categoryId
              AND s.id IN :serviceIds
            """)
    List<ServiceListing> findByCategoryIdAndIdIn(@Param("categoryId") Long categoryId,
                                                 @Param("serviceIds") List<Long> serviceIds);

    @Query("""
            SELECT new com.quickserv.quickserv.dto.search.ServiceSearchResultDto(
                p.providerId,
                u.name,
                COALESCE(p.businessName, u.name),
                COALESCE(p.rating, 0.0),
                s.id,
                COALESCE(sc.name, s.title),
                s.price,
                c.id,
                c.name,
                sc.id,
                sc.name,
                u.location,
                p.latitude,
                p.longitude,
                CONCAT('/service/', s.id)
            )
            FROM ServiceListing s
            JOIN s.provider u
            JOIN s.category c
            LEFT JOIN s.subcategory sc
            JOIN Provider p ON p.user = u
            WHERE s.isAvailable = true
              AND (:location IS NULL OR TRIM(:location) = '' OR LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId OR p.category.id = :categoryId)
              AND (:subcategoryId IS NULL OR sc.id = :subcategoryId)
              AND (:minPrice IS NULL OR s.price >= :minPrice)
              AND (:maxPrice IS NULL OR s.price <= :maxPrice)
              AND (:minRating IS NULL OR COALESCE(p.rating, 0.0) >= :minRating)
            ORDER BY COALESCE(p.rating, 0.0) DESC, s.price ASC
            """)
    List<ServiceSearchResultDto> searchDiscovery(@Param("location") String location,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("subcategoryId") Long subcategoryId,
                                                 @Param("minPrice") BigDecimal minPrice,
                                                 @Param("maxPrice") BigDecimal maxPrice,
                                                 @Param("minRating") Double minRating);

    @Query("""
            SELECT s
            FROM ServiceListing s
            LEFT JOIN s.subcategory sc
            WHERE COALESCE(s.isAvailable, true) = true
              AND (:keyword IS NULL OR TRIM(:keyword) = ''
                   OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR TRIM(:location) = ''
                   OR LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%'))
                   OR FUNCTION('LOCATE',
                               CONCAT(',', LOWER(FUNCTION('REPLACE', TRIM(:location), ' ', '')), ','),
                               CONCAT(',', LOWER(FUNCTION('REPLACE', COALESCE(s.serviceLocations, s.location, ''), ' ', '')), ',')) > 0)
              AND (:categoryId IS NULL OR s.category.id = :categoryId)
              AND (:subcategoryId IS NULL
                   OR sc.id = :subcategoryId
                   OR (sc IS NULL AND EXISTS (
                        SELECT 1
                        FROM Subcategory sub
                        WHERE sub.id = :subcategoryId
                          AND (LOWER(s.title) LIKE LOWER(CONCAT('%', sub.name, '%'))
                               OR LOWER(s.description) LIKE LOWER(CONCAT('%', sub.name, '%')))
                   )))
              AND (:minPrice IS NULL OR s.price >= :minPrice)
              AND (:maxPrice IS NULL OR s.price <= :maxPrice)
            ORDER BY s.createdAt DESC
            """)
    List<ServiceListing> browseWithFilters(@Param("keyword") String keyword,
                                           @Param("location") String location,
                                           @Param("categoryId") Long categoryId,
                                           @Param("subcategoryId") Long subcategoryId,
                                           @Param("minPrice") BigDecimal minPrice,
                                           @Param("maxPrice") BigDecimal maxPrice);

    void deleteByProvider(User provider);
}
