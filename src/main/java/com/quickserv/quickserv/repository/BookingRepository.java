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

    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Booking> findByProviderOrderByCreatedAtDesc(User provider);

    List<Booking> findByProviderAndStatusOrderByBookingDateTimeAsc(User provider, Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.provider = :provider AND b.bookingDateTime > :now AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.bookingDateTime ASC")
    List<Booking> findUpcomingBookingsForProvider(@Param("provider") User provider, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.provider = :provider AND b.bookingDateTime >= :startOfDay AND b.bookingDateTime < :endOfDay ORDER BY b.bookingDateTime ASC")
    List<Booking> findTodaysBookingsForProvider(@Param("provider") User provider, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.provider = :provider AND b.status IN ('PENDING', 'CONFIRMED') AND b.bookingDateTime BETWEEN :startTime AND :endTime")
    long countConflictingBookings(@Param("provider") User provider, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}

