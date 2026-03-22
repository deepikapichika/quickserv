package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
import com.quickserv.quickserv.dto.booking.BookingCreateRequest;
import com.quickserv.quickserv.dto.booking.BookingResponse;
import com.quickserv.quickserv.dto.booking.BookingStatusUpdateRequest;
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
 * REST Controller for Booking Management
 * Handles all booking-related API endpoints for customers and providers
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * CUSTOMER ENDPOINTS
     */

    /**
     * Create a new booking (Customer only)
     * POST /api/bookings/create
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

            if (!"CUSTOMER".equals(customer.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only customers can create bookings"));
            }

            Booking booking = bookingService.createBooking(customer, request);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createSuccessResponse("Booking created successfully", response));

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating booking: " + e.getMessage()));
        }
    }

    /**
     * Get all bookings for logged-in customer
     * GET /api/bookings/my-bookings
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            List<Booking> bookings = bookingService.getCustomerBookings(customer);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Get customer bookings by status
     * GET /api/bookings/my-bookings/status/{status}
     */
    @GetMapping("/my-bookings/status/{status}")
    public ResponseEntity<?> getMyBookingsByStatus(
            @PathVariable String status,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            try {
                Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
                List<Booking> bookings = bookingService.getCustomerBookingsByStatus(customer, bookingStatus);
                List<BookingResponse> responses = bookings.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());

                return ResponseEntity.ok(createSuccessResponse("Bookings retrieved successfully", responses));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Invalid booking status: " + status));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Get upcoming bookings for customer
     * GET /api/bookings/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingBookings(HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            List<Booking> bookings = bookingService.getCustomerUpcomingBookings(customer);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Upcoming bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Get booking details by ID
     * GET /api/bookings/{bookingId}
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingDetails(
            @PathVariable Long bookingId,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            // Verify ownership (customer or provider)
            if (!booking.getCustomer().getId().equals(user.getId()) &&
                !booking.getProvider().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("You don't have permission to view this booking"));
            }

            BookingResponse response = convertToResponse(booking);
            return ResponseEntity.ok(createSuccessResponse("Booking details retrieved successfully", response));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving booking: " + e.getMessage()));
        }
    }

    /**
     * Cancel booking (Customer only)
     * POST /api/bookings/{bookingId}/cancel
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = bookingService.cancelBooking(bookingId, customer);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Booking cancelled successfully", response));

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error cancelling booking: " + e.getMessage()));
        }
    }

    /**
     * Reschedule booking (Customer only)
     * POST /api/bookings/{bookingId}/reschedule
     */
    @PostMapping("/{bookingId}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable Long bookingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime,
            HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            Booking booking = bookingService.rescheduleBooking(bookingId, newDateTime, customer);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Booking rescheduled successfully", response));

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error rescheduling booking: " + e.getMessage()));
        }
    }

    /**
     * PROVIDER ENDPOINTS
     */

    /**
     * Get all bookings for logged-in provider
     * GET /api/bookings/provider/all
     */
    @GetMapping("/provider/all")
    public ResponseEntity<?> getProviderBookings(HttpSession session) {
        try {
            User provider = (User) session.getAttribute("loggedInUser");
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"PROVIDER".equals(provider.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only providers can access this endpoint"));
            }

            List<Booking> bookings = bookingService.getProviderBookings(provider);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Provider bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Get upcoming bookings for provider
     * GET /api/bookings/provider/upcoming
     */
    @GetMapping("/provider/upcoming")
    public ResponseEntity<?> getProviderUpcomingBookings(HttpSession session) {
        try {
            User provider = (User) session.getAttribute("loggedInUser");
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"PROVIDER".equals(provider.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only providers can access this endpoint"));
            }

            List<Booking> bookings = bookingService.getUpcomingBookings(provider);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Upcoming bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Get today's bookings for provider
     * GET /api/bookings/provider/today
     */
    @GetMapping("/provider/today")
    public ResponseEntity<?> getProviderTodaysBookings(HttpSession session) {
        try {
            User provider = (User) session.getAttribute("loggedInUser");
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"PROVIDER".equals(provider.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only providers can access this endpoint"));
            }

            List<Booking> bookings = bookingService.getTodaysBookings(provider);
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("Today's bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Update booking status (Provider only)
     * POST /api/bookings/{bookingId}/update-status
     */
    @PostMapping("/{bookingId}/update-status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingStatusUpdateRequest request,
            HttpSession session) {
        try {
            User provider = (User) session.getAttribute("loggedInUser");
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"PROVIDER".equals(provider.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only providers can update booking status"));
            }

            Booking.BookingStatus status = Booking.BookingStatus.valueOf(request.getStatus().toUpperCase());
            Booking booking = bookingService.updateBookingStatus(bookingId, status, request.getProviderNotes(), provider);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Booking status updated successfully", response));

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating booking: " + e.getMessage()));
        }
    }

    /**
     * Add notes to booking (Provider only)
     * POST /api/bookings/{bookingId}/add-notes
     */
    @PostMapping("/{bookingId}/add-notes")
    public ResponseEntity<?> addNotesToBooking(
            @PathVariable Long bookingId,
            @RequestParam String notes,
            HttpSession session) {
        try {
            User provider = (User) session.getAttribute("loggedInUser");
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"PROVIDER".equals(provider.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only providers can add notes"));
            }

            Booking booking = bookingService.addProviderNotes(bookingId, notes, provider);
            BookingResponse response = convertToResponse(booking);

            return ResponseEntity.ok(createSuccessResponse("Notes added successfully", response));

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error adding notes: " + e.getMessage()));
        }
    }

    /**
     * ADMIN ENDPOINTS
     */

    /**
     * Get all bookings (Admin only)
     * GET /api/bookings/admin/all
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllBookings(HttpSession session) {
        try {
            User admin = (User) session.getAttribute("loggedInUser");
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not logged in"));
            }

            if (!"ADMIN".equals(admin.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Only admins can access this endpoint"));
            }

            List<Booking> bookings = bookingService.getAllBookings();
            List<BookingResponse> responses = bookings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(createSuccessResponse("All bookings retrieved successfully", responses));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving bookings: " + e.getMessage()));
        }
    }

    /**
     * Helper Methods
     */

    /**
     * Convert Booking entity to BookingResponse DTO
     */
    private BookingResponse convertToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setCustomerId(booking.getCustomer().getId());
        response.setCustomerName(booking.getCustomer().getName());
        response.setCustomerEmail(booking.getCustomer().getEmail());
        response.setProviderId(booking.getProvider().getId());
        response.setProviderName(booking.getProvider().getName());
        response.setProviderEmail(booking.getProvider().getEmail());
        response.setServiceId(booking.getService().getId());
        response.setServiceName(booking.getService().getTitle());
        response.setServiceDescription(booking.getService().getDescription());
        response.setServicePrice(booking.getService().getPrice());
        response.setPriceUnit(booking.getService().getPriceUnit());
        response.setBookingDateTime(booking.getBookingDateTime());
        response.setCustomerNotes(booking.getCustomerNotes());
        response.setProviderNotes(booking.getProviderNotes());
        response.setStatus(booking.getStatus().toString());
        response.setTotalAmount(booking.getTotalAmount());
        response.setDiscountAmount(booking.getDiscountAmount());
        if (booking.getPaymentMethod() != null) {
            response.setPaymentMethod(booking.getPaymentMethod().toString());
        }
        response.setCouponCode(booking.getCouponCode());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }

    /**
     * Create success response wrapper
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * Create error response wrapper
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

