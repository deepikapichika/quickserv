package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Review;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.BookingRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProviderRepository providerRepository;

    /**
     * Create a review for a completed booking
     */
    @Transactional
    public Review createReview(Long bookingId, User customer, Integer rating, String comment) {
        // Validate rating
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessValidationException("Rating must be between 1 and 5");
        }

        // Fetch booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Verify booking is completed
        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new BusinessValidationException("Can only review completed bookings");
        }

        // Verify customer owns the booking
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessValidationException("You can only review your own bookings");
        }

        // Check if review already exists
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new BusinessValidationException("This booking has already been reviewed");
        }

        // Create review
        Review review = new Review(booking, customer, booking.getProvider(), rating, comment);
        Review savedReview = reviewRepository.save(review);

        // Update provider aggregate rating/count for consistency across profile/search views
        if (booking.getProvider() != null) {
            com.quickserv.quickserv.entity.Provider provider = providerRepository.findByUser(booking.getProvider());
            if (provider != null) {
                int currentTotalReviews = provider.getTotalReviews() != null ? provider.getTotalReviews() : 0;
                double currentRating = provider.getRating() != null ? provider.getRating() : 0.0;
                double newAverage = ((currentRating * currentTotalReviews) + rating) / (currentTotalReviews + 1);
                provider.setRating(Math.round(newAverage * 10.0) / 10.0);
                provider.setTotalReviews(currentTotalReviews + 1);
                providerRepository.save(provider);
            }
        }

        return savedReview;
    }

    /**
     * Get all reviews for a provider
     */
    public List<Review> getProviderReviews(User provider) {
        return reviewRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

    /**
     * Get average rating for a provider
     */
    public Double getProviderAverageRating(Long providerId) {
        Double average = reviewRepository.getAverageRatingForProvider(providerId);
        return average != null ? Math.round(average * 10.0) / 10.0 : 0.0;
    }

    /**
     * Get review count for a provider
     */
    public Long getProviderReviewCount(Long providerId) {
        return reviewRepository.getReviewCountForProvider(providerId);
    }

    /**
     * Get review for a specific booking
     */
    public Optional<Review> getReviewByBooking(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    public List<Long> getReviewedBookingIdsForCustomer(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return reviewRepository.findReviewedBookingIdsByCustomerId(customerId);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(Long reviewId, User customer) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessValidationException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }
}
