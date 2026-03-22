package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.booking.AdvancedBookingService;
import com.quickserv.quickserv.dto.booking.BookingCreateRequest;
import com.quickserv.quickserv.dto.booking.BookingResponse;
import com.quickserv.quickserv.dto.booking.PricingBreakdownDto;
import com.quickserv.quickserv.dto.booking.RefundCalculationDto;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advanced Booking Controller - Complete booking lifecycle management
 * Handles customer and admin booking operations
 */
@RestController
@RequestMapping("/api/advanced/bookings")
public class AdvancedBookingController {

    @Autowired
    private AdvancedBookingService advancedBookingService;

    // ==================== CUSTOMER ENDPOINTS ====================

    /**
     * Create new booking
     * POST /api/advanced/bookings/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = advancedBookingService.createBooking(customer, request);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("Booking created successfully", response));

        } catch (BusinessValidationException | ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating booking: " + e.getMessage()));
        }
    }

    /**
     * Confirm booking
     * POST /api/advanced/bookings/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmBooking(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = advancedBookingService.confirmBooking(id, customer);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Booking confirmed successfully", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Cancel booking
     * POST /api/advanced/bookings/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = advancedBookingService.cancelBooking(id, customer);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking cancelled successfully");
            response.put("refundAmount", booking.getRefundAmount());
            response.put("refundReason", booking.getRefundReason());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Reschedule booking
     * POST /api/advanced/bookings/{id}/reschedule?newDateTime=...
     */
    @PostMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = advancedBookingService.rescheduleBooking(id, newDateTime, customer);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Booking rescheduled successfully", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get booking details
     * GET /api/advanced/bookings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingDetails(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = advancedBookingService.getBookingById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            // Verify ownership
            if (!booking.getCustomer().getId().equals(user.getId()) &&
                (booking.getProvider() == null || !booking.getProvider().getId().equals(user.getId()))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("You don't have permission to view this booking"));
            }

            BookingResponse response = convertToResponse(booking);
            return ResponseEntity.ok(createSuccessResponse("Booking details retrieved", response));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving booking: " + e.getMessage()));
        }
    }

    /**
     * Get pricing breakdown
     * GET /api/advanced/bookings/{id}/pricing
     */
    @GetMapping("/{id}/pricing")
    public ResponseEntity<?> getPricingBreakdown(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            PricingBreakdownDto pricing = advancedBookingService.getPricingBreakdown(id);
            return ResponseEntity.ok(createSuccessResponse("Pricing breakdown retrieved", pricing));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving pricing: " + e.getMessage()));
        }
    }

    /**
     * Get my bookings
     * GET /api/advanced/bookings/my-bookings
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            List<Booking> bookings = advancedBookingService.getCustomerBookings(customer);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Bookings retrieved", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Handle provider no-show
     * POST /api/advanced/bookings/admin/{id}/no-show
     */
    @PostMapping("/admin/{id}/no-show")
    public ResponseEntity<?> handleProviderNoShow(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User admin = (User) session.getAttribute("loggedInUser");
            if (admin == null || !"ADMIN".equals(admin.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Admin access required"));
            }

            Booking booking = advancedBookingService.handleProviderNoShow(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Provider no-show processed");
            response.put("refundAmount", booking.getRefundAmount());
            response.put("creditAmount", "₹100");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get refund calculation
     * GET /api/advanced/bookings/{id}/refund
     */
    @GetMapping("/{id}/refund")
    public ResponseEntity<?> getRefundCalculation(
            @PathVariable Long id,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            RefundCalculationDto refund = advancedBookingService.getRefundCalculation(id);
            return ResponseEntity.ok(createSuccessResponse("Refund calculation retrieved", refund));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private BookingResponse convertToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setCustomerId(booking.getCustomer().getId());
        response.setCustomerName(booking.getCustomer().getName());
        if (booking.getProvider() != null) {
            response.setProviderId(booking.getProvider().getId());
            response.setProviderName(booking.getProvider().getName());
        }
        response.setServiceId(booking.getService().getId());
        response.setServiceName(booking.getService().getTitle());
        response.setBookingDateTime(booking.getBookingDateTime());
        response.setStatus(booking.getStatus().toString());
        response.setTotalAmount(booking.getTotalAmount());
        response.setBasePrice(booking.getBasePrice());
        response.setTravelCharge(booking.getTravelCharge());
        response.setAddonCharges(booking.getAddonCharges());
        response.setCouponDiscount(booking.getCouponDiscount());
        response.setGstAmount(booking.getGstAmount());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

