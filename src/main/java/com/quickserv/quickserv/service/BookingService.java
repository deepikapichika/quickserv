package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.PaymentMethod;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.BookingRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.dto.booking.BookingCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Map<Long, BigDecimal> ADDON_PRICE_MAP = createAddonPriceMap();

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ProviderRepository providerRepository;

    /**
     * Create a new booking with validation and conflict checking
     */
    @Transactional
    public Booking createBooking(User customer, BookingCreateRequest request) {
        // Validate booking request
        if (request.getServiceId() == null) {
            throw new BusinessValidationException("Service ID is required");
        }
        if (request.getBookingDateTime() == null) {
            throw new BusinessValidationException("Booking date and time is required");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            throw new BusinessValidationException("Payment method is required");
        }

        // Check if booking date is in the future
        if (request.getBookingDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Booking date and time must be in the future");
        }

        // Fetch service
        ServiceListing service = serviceService.getServiceByIdAsOptional(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + request.getServiceId()));

        if (!service.getIsAvailable()) {
            throw new BusinessValidationException("Service is not available at this time");
        }

        // Check for booking conflicts
        if (hasBookingConflict(service.getProvider(), request.getBookingDateTime())) {
            throw new BusinessValidationException("Provider is not available at this time. Please select a different time slot.");
        }

        // Create booking
        Booking booking = new Booking(customer, service.getProvider(), service, request.getBookingDateTime());
        booking.setCustomerNotes(request.getCustomerNotes());
        booking.setPaymentMethod(parsePaymentMethod(request.getPaymentMethod()));
        booking.setAddonIds(convertAddonsToString(request.getAddonIds()));
        booking.setCouponCode(normalizeCouponCode(request.getCouponCode()));

        booking.setCustomerAddress(firstNonBlank(request.getCustomerAddress(), customer.getLocation()));
        booking.setCustomerLatitude(request.getCustomerLatitude() != null ? request.getCustomerLatitude() : customer.getLatitude());
        booking.setCustomerLongitude(request.getCustomerLongitude() != null ? request.getCustomerLongitude() : customer.getLongitude());

        com.quickserv.quickserv.entity.Provider providerProfile = providerRepository.findByUser(service.getProvider());
        booking.setProviderAddress(firstNonBlank(
                providerProfile != null ? providerProfile.getProviderLocations() : null,
                service.getServiceLocations(),
                service.getLocation(),
                service.getProvider() != null ? service.getProvider().getLocation() : null
        ));
        booking.setProviderLatitude(providerProfile != null ? providerProfile.getLatitude() : null);
        booking.setProviderLongitude(providerProfile != null ? providerProfile.getLongitude() : null);

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(service, request);
        booking.setTotalAmount(totalAmount);

        return bookingRepository.save(booking);
    }

    // Legacy method for backward compatibility
    public Booking createBooking(User customer, ServiceListing service, LocalDateTime bookingDateTime, String notes) {
        BookingCreateRequest request = new BookingCreateRequest(service.getId(), bookingDateTime, "CASH");
        request.setCustomerNotes(notes);
        return createBooking(customer, request);
    }

    // Get all bookings for a customer
    public List<Booking> getCustomerBookings(User customer) {
        return bookingRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Booking> getCustomerBookingsByStatus(User customer, Booking.BookingStatus status) {
        return bookingRepository.findByCustomerAndStatusOrderByBookingDateTimeDesc(customer, status);
    }

    public List<Booking> getCustomerUpcomingBookings(User customer) {
        return bookingRepository.findUpcomingBookingsForCustomer(customer, LocalDateTime.now());
    }

    // Get all bookings for a provider
    public List<Booking> getProviderBookings(User provider) {
        return bookingRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

    // Get all bookings platform-wide (admin)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
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

    @Transactional
    public Booking updateBookingStatus(Long bookingId, Booking.BookingStatus status, User provider) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new BusinessValidationException("Unauthorized: You can only update your own bookings");
        }

        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateBookingStatus(Long bookingId, Booking.BookingStatus status, String providerNotes, User provider) {
        Booking booking = updateBookingStatus(bookingId, status, provider);
        if (providerNotes != null && !providerNotes.isEmpty()) {
            booking.setProviderNotes(providerNotes);
            booking = bookingRepository.save(booking);
        }
        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, User customer) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessValidationException("Unauthorized: You can only cancel your own bookings");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new BusinessValidationException("Cannot cancel a completed booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BusinessValidationException("Booking is already cancelled");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking rescheduleBooking(Long bookingId, LocalDateTime newDateTime, User customer) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessValidationException("Unauthorized: You can only reschedule your own bookings");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED || booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BusinessValidationException("Cannot reschedule a " + booking.getStatus().toString().toLowerCase() + " booking");
        }

        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("New booking date must be in the future");
        }

        if (hasBookingConflict(booking.getProvider(), newDateTime)) {
            throw new BusinessValidationException("Provider is not available at the new time. Please select a different time slot.");
        }

        booking.setBookingDateTime(newDateTime);
        booking.setStatus(Booking.BookingStatus.RESCHEDULED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking addProviderNotes(Long bookingId, String notes, User provider) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getProvider().getId().equals(provider.getId())) {
            throw new BusinessValidationException("Unauthorized: You can only add notes to your own bookings");
        }

        booking.setProviderNotes(notes);
        return bookingRepository.save(booking);
    }

    private boolean hasBookingConflict(User provider, LocalDateTime bookingDateTime) {
        LocalDateTime endTime = bookingDateTime.plusHours(1);
        long conflictingCount = bookingRepository.countConflictingBookings(provider, bookingDateTime, endTime);
        return conflictingCount > 0;
    }

    private BigDecimal calculateTotalAmount(ServiceListing service, BookingCreateRequest request) {
        BigDecimal basePrice = service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO;
        BigDecimal addonCharges = calculateAddonCharges(request.getAddonIds());

        BigDecimal subtotal = basePrice.add(addonCharges);
        BigDecimal discount = calculateCouponDiscount(service, request.getCouponCode(), subtotal);
        BigDecimal total = subtotal.subtract(discount);

        if (total.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return total;
    }

    private BigDecimal calculateAddonCharges(List<Long> addonIds) {
        if (addonIds == null || addonIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return addonIds.stream()
                .map(id -> ADDON_PRICE_MAP.getOrDefault(id, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCouponDiscount(ServiceListing service, String rawCouponCode, BigDecimal amountBeforeDiscount) {
        String couponCode = normalizeCouponCode(rawCouponCode);
        if (couponCode == null || amountBeforeDiscount.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        if ("FIRST30".equals(couponCode)) {
            return amountBeforeDiscount.multiply(new BigDecimal("0.30"));
        }

        if (service.getCouponCode() == null || service.getDiscountPercent() == null) {
            return BigDecimal.ZERO;
        }

        String serviceCoupon = service.getCouponCode().trim().toUpperCase();
        if (!serviceCoupon.equals(couponCode)) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = service.getDiscountPercent().movePointLeft(2);
        return amountBeforeDiscount.multiply(percent);
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new BusinessValidationException("Payment method is required");
        }
        try {
            return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid payment method. Use CARD, UPI, WALLET, or CASH");
        }
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }

    private static Map<Long, BigDecimal> createAddonPriceMap() {
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(1L, new BigDecimal("99"));
        map.put(2L, new BigDecimal("149"));
        map.put(3L, new BigDecimal("199"));
        map.put(4L, new BigDecimal("249"));
        map.put(5L, new BigDecimal("129"));
        return map;
    }

    private String convertAddonsToString(List<Long> addonIds) {
        if (addonIds == null || addonIds.isEmpty()) {
            return null;
        }
        return addonIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public BookingStats getBookingStats(User provider) {
        List<Booking> allBookings = getProviderBookings(provider);

        long totalBookings = allBookings.size();
        long pendingBookings = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING)
                .count();
        long todaysBookings = getTodaysBookings(provider).size();
        long upcomingBookings = getUpcomingBookings(provider).size();

        return new BookingStats(totalBookings, pendingBookings, todaysBookings, upcomingBookings);
    }

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

        public long getTotalBookings() { return totalBookings; }
        public long getPendingBookings() { return pendingBookings; }
        public long getTodaysBookings() { return todaysBookings; }
        public long getUpcomingBookings() { return upcomingBookings; }
    }
}
