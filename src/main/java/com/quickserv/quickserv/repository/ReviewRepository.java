package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Review;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProvider(User provider);

    List<Review> findByProviderOrderByCreatedAtDesc(User provider);

    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.provider.id = :providerId")
    Double getAverageRatingForProvider(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.provider.id = :providerId")
    Long getReviewCountForProvider(@Param("providerId") Long providerId);

    void deleteByBookingIdIn(List<Long> bookingIds);

    void deleteByCustomerOrProvider(User customer, User provider);
}
