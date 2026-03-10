package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    // Create a new booking
    public Booking createBooking(User customer, ServiceListing service, LocalDateTime bookingDateTime, String notes) {
        // Check for booking conflicts
        if (hasBookingConflict(service.getProvider(), bookingDateTime)) {
            throw new RuntimeException("Provider is not available at this time");
        }

        Booking booking = new Booking(customer, service.getProvider(), service, bookingDateTime);
        booking.setCustomerNotes(notes);
        booking.setTotalAmount(service.getPrice());

        return bookingRepository.save(booking);
    }

    // Get all bookings for a provider
    public List<Booking> getProviderBookings(User provider) {
        return bookingRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

    // Get upcoming bookings for a provider
    public List<Booking> getUpcomingBookings(User provider) {
        return bookingRepository.findUpcomingBookingsForProvider(provider, LocalDateTime.now());
    }

    // Get today's bookings for a provider
    public List<Booking> getTodaysBookings(User provider) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return bookingRepository.findTodaysBookingsForProvider(provider, startOfDay, endOfDay);
    }

    // Update booking status
    public Booking updateBookingStatus(Long bookingId, Booking.BookingStatus status, User provider) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Ensure only the provider can update their bookings
        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized to update this booking");
        }

        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    // Add provider notes to booking
    public Booking addProviderNotes(Long bookingId, String notes, User provider) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized to update this booking");
        }

        booking.setProviderNotes(notes);
        return bookingRepository.save(booking);
    }

    // Check for booking conflicts
    private boolean hasBookingConflict(User provider, LocalDateTime bookingDateTime) {
        LocalDateTime endTime = bookingDateTime.plusHours(1); // Assuming 1-hour services
        long conflictingCount = bookingRepository.countConflictingBookings(provider, bookingDateTime, endTime);
        return conflictingCount > 0;
    }

    // Get booking statistics for provider dashboard
    public BookingStats getBookingStats(User provider) {
        List<Booking> allBookings = getProviderBookings(provider);
        LocalDateTime now = LocalDateTime.now();

        long totalBookings = allBookings.size();
        long pendingBookings = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING)
                .count();
        long todaysBookings = getTodaysBookings(provider).size();
        long upcomingBookings = getUpcomingBookings(provider).size();

        return new BookingStats(totalBookings, pendingBookings, todaysBookings, upcomingBookings);
    }

    // Inner class for booking statistics
    public static class BookingStats {
        private long totalBookings;
        private long pendingBookings;
        private long todaysBookings;
        private long upcomingBookings;

        public BookingStats(long totalBookings, long pendingBookings, long todaysBookings, long upcomingBookings) {
            this.totalBookings = totalBookings;
            this.pendingBookings = pendingBookings;
            this.todaysBookings = todaysBookings;
            this.upcomingBookings = upcomingBookings;
        }

        // Getters
        public long getTotalBookings() { return totalBookings; }
        public long getPendingBookings() { return pendingBookings; }
        public long getTodaysBookings() { return todaysBookings; }
        public long getUpcomingBookings() { return upcomingBookings; }
    }

    public List<Booking> getCustomerBookings(User customer) {
        return bookingRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public void cancelBooking(Long bookingId, User customer) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed booking");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public Booking updateBookingStatus(Long bookingId, Booking.BookingStatus status,
                                       User provider, String providerNotes) {
        Booking booking = getBookingById(bookingId);

        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("Unauthorized to update this booking");
        }

        booking.setStatus(status);
        if (providerNotes != null && !providerNotes.isEmpty()) {
            booking.setProviderNotes(providerNotes);
        }
        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public long getTotalBookings() {
        return bookingRepository.count();
    }
}

