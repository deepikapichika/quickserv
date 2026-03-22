package com.quickserv.quickserv.repository;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings for a customer
    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);

    // Find all bookings for a provider
    List<Booking> findByProviderOrderByCreatedAtDesc(User provider);

    // Find bookings by status for a provider
    List<Booking> findByProviderAndStatusOrderByBookingDateTimeAsc(User provider, Booking.BookingStatus status);

    // Find bookings by status for a customer
    List<Booking> findByCustomerAndStatusOrderByBookingDateTimeDesc(User customer, Booking.BookingStatus status);

    // Find upcoming bookings for a provider
    @Query("SELECT b FROM Booking b WHERE b.provider = :provider AND b.bookingDateTime > :now AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.bookingDateTime ASC")
    List<Booking> findUpcomingBookingsForProvider(@Param("provider") User provider, @Param("now") LocalDateTime now);

    // Find upcoming bookings for a customer
    @Query("SELECT b FROM Booking b WHERE b.customer = :customer AND b.bookingDateTime > :now AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.bookingDateTime ASC")
    List<Booking> findUpcomingBookingsForCustomer(@Param("customer") User customer, @Param("now") LocalDateTime now);

    // Find today's bookings for a provider
    @Query("SELECT b FROM Booking b WHERE b.provider = :provider AND b.bookingDateTime >= :startOfDay AND b.bookingDateTime < :endOfDay ORDER BY b.bookingDateTime ASC")
    List<Booking> findTodaysBookingsForProvider(@Param("provider") User provider, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Check for booking conflicts (same provider, overlapping time)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.provider = :provider AND b.status IN ('PENDING', 'CONFIRMED') AND " +
           "b.bookingDateTime BETWEEN :startTime AND :endTime")
    long countConflictingBookings(@Param("provider") User provider,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    // Find bookings by coupon code
    @Query("SELECT b FROM Booking b WHERE b.couponCode = :couponCode")
    List<Booking> findByCouponCode(@Param("couponCode") String couponCode);

    // Find bookings by customer or provider
    List<Booking> findByCustomerOrProvider(User customer, User provider);

    // Count bookings by customer or provider
    long countByCustomerOrProvider(User customer, User provider);

    // Delete bookings by customer or provider
    void deleteByCustomerOrProvider(User customer, User provider);
}
