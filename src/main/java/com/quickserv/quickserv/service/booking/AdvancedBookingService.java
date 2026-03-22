package com.quickserv.quickserv.service.booking;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.PaymentMethod;
import com.quickserv.quickserv.entity.ServiceListing;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.dto.booking.BookingCreateRequest;
import com.quickserv.quickserv.dto.booking.PricingBreakdownDto;
import com.quickserv.quickserv.dto.booking.RefundCalculationDto;
import com.quickserv.quickserv.exception.BookingException;
import com.quickserv.quickserv.exception.InvalidBookingStateException;
import com.quickserv.quickserv.repository.BookingRepository;
import com.quickserv.quickserv.repository.ServiceRepository;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Advanced Booking Service - Orchestrates complete booking lifecycle
 * Handles: creation, confirmation, cancellation, rescheduling, refunds
 */
@Service
@Transactional
public class AdvancedBookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private CancellationService cancellationService;

    /**
     * Create new booking with full validation and pricing
     *
     * @param customer Customer making the booking
     * @param request Booking creation request
     * @return Created booking with pricing calculated
     */
    public Booking createBooking(User customer, BookingCreateRequest request) {
        // Validate customer
        if (customer == null) {
            throw new BusinessValidationException("Customer is required");
        }

        // Fetch service
        ServiceListing service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service not found with ID: " + request.getServiceId()
            ));

        // Validate booking date is in future
        if (request.getBookingDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException(
                "Booking date and time must be in the future"
            );
        }

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setBookingDateTime(request.getBookingDateTime());
        booking.setCustomerNotes(request.getCustomerNotes());
        booking.setPaymentMethod(request.getPaymentMethod() != null ?
            PaymentMethod.valueOf(request.getPaymentMethod()) : null);

        // Convert addon IDs from List<Long> to comma-separated String
        if (request.getAddonIds() != null && !request.getAddonIds().isEmpty()) {
            String addonIdsStr = String.join(",",
                request.getAddonIds().stream()
                    .map(String::valueOf)
                    .toArray(String[]::new));
            booking.setAddonIds(addonIdsStr);
        }

        booking.setCouponCode(request.getCouponCode());
        booking.setBasePrice(service.getPrice());
        booking.setStatus(Booking.BookingStatus.PENDING);

        // Calculate pricing
        booking = pricingService.calculateTotalAmount(booking);

        // Save booking
        booking = bookingRepository.save(booking);

        return booking;
    }

    /**
     * Confirm booking (customer confirmation)
     * Transition: PENDING → CONFIRMED
     *
     * @param bookingId Booking to confirm
     * @param customer Customer confirming
     * @return Updated booking
     */
    public Booking confirmBooking(Long bookingId, User customer) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        // Verify ownership
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BookingException("You cannot confirm this booking", "UNAUTHORIZED");
        }

        // Validate state transition
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new InvalidBookingStateException(
                booking.getStatus().toString(),
                Booking.BookingStatus.CONFIRMED.toString()
            );
        }

        // Update status
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    /**
     * Cancel booking with refund
     *
     * @param bookingId Booking to cancel
     * @param customer Customer cancelling
     * @return Updated booking with refund details
     */
    public Booking cancelBooking(Long bookingId, User customer) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        // Verify ownership
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BookingException("You cannot cancel this booking", "UNAUTHORIZED");
        }

        // Process cancellation
        booking = cancellationService.processCancellation(
            booking,
            "Customer cancellation",
            false
        );

        return bookingRepository.save(booking);
    }

    /**
     * Handle provider no-show (admin/system action)
     *
     * @param bookingId Booking with no-show
     * @return Updated booking with full refund + credit
     */
    public Booking handleProviderNoShow(Long bookingId) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        // Process cancellation with no-show policy
        booking = cancellationService.processCancellation(
            booking,
            "Provider did not show up",
            true
        );

        return bookingRepository.save(booking);
    }

    /**
     * Reschedule booking to new date/time
     *
     * @param bookingId Booking to reschedule
     * @param newDateTime New booking date/time
     * @param customer Customer rescheduling
     * @return Updated booking with new time
     */
    public Booking rescheduleBooking(Long bookingId, LocalDateTime newDateTime, User customer) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        // Verify ownership
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BookingException("You cannot reschedule this booking", "UNAUTHORIZED");
        }

        // Validate can reschedule
        if (!booking.canBeRescheduled()) {
            throw new InvalidBookingStateException(
                booking.getStatus().toString(),
                "RESCHEDULED"
            );
        }

        // Validate new date is in future
        if (newDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException(
                "New booking date must be in the future"
            );
        }

        // Update booking
        booking.setBookingDateTime(newDateTime);
        booking.setStatus(Booking.BookingStatus.RESCHEDULED);
        booking.setRescheduledAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    /**
     * Get pricing breakdown for a booking
     *
     * @param bookingId Booking ID
     * @return Pricing breakdown DTO
     */
    public PricingBreakdownDto getPricingBreakdown(Long bookingId) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        PricingBreakdownDto dto = new PricingBreakdownDto(
            booking.getBasePrice(),
            booking.getTravelCharge(),
            booking.getAddonCharges(),
            booking.getCouponDiscount(),
            booking.getGstAmount(),
            booking.getTotalAmount()
        );

        dto.setDistanceKm(booking.getDistanceKm());
        dto.setCouponCode(booking.getCouponCode());
        dto.setDescription(dto.getPricingSummary());

        return dto;
    }

    /**
     * Get refund calculation for a booking
     *
     * @param bookingId Booking ID
     * @return Refund calculation DTO
     */
    public RefundCalculationDto getRefundCalculation(Long bookingId) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        BigDecimal refundAmount = cancellationService.calculateRefund(booking);

        RefundCalculationDto dto = new RefundCalculationDto(
            booking.getId(),
            booking.getTotalAmount(),
            refundAmount,
            "Cancellation policy applied"
        );

        dto.setRefundPolicy(cancellationService.getRefundPolicyDescription());
        dto.setHoursUntilService(calculateHoursUntilService(booking.getBookingDateTime()));

        return dto;
    }

    /**
     * Complete booking (mark as completed)
     * Transition: IN_PROGRESS → COMPLETED
     *
     * @param bookingId Booking to complete
     * @return Completed booking
     */
    public Booking completeBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found with ID: " + bookingId
            ));

        if (booking.getStatus() != Booking.BookingStatus.IN_PROGRESS) {
            throw new InvalidBookingStateException(
                booking.getStatus().toString(),
                Booking.BookingStatus.COMPLETED.toString()
            );
        }

        booking.setStatus(Booking.BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    /**
     * Get booking by ID with ownership check
     *
     * @param bookingId Booking ID
     * @return Optional containing booking if found
     */
    public Optional<Booking> getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }

    /**
     * Get all bookings for customer
     *
     * @param customer Customer to fetch bookings for
     * @return List of customer bookings
     */
    public List<Booking> getCustomerBookings(User customer) {
        return bookingRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    /**
     * Get bookings for provider
     *
     * @param provider Provider to fetch bookings for
     * @return List of provider bookings
     */
    public List<Booking> getProviderBookings(User provider) {
        return bookingRepository.findByProviderOrderByCreatedAtDesc(provider);
    }

    /**
     * Calculate hours until service
     *
     * @param bookingDateTime Service date/time
     * @return Human-readable hours string
     */
    private String calculateHoursUntilService(LocalDateTime bookingDateTime) {
        long hours = java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime);
        if (hours < 0) {
            return "Service already occurred";
        }
        return hours + " hours remaining";
    }
}






