package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Payment;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.BookingService;
import com.quickserv.quickserv.service.EmailService;
import com.quickserv.quickserv.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;

    /**
     * Create Razorpay order for booking
     * POST /api/payments/create-order/{bookingId}
     */
    @PostMapping("/create-order/{bookingId}")
    public ResponseEntity<?> createPaymentOrder(@PathVariable Long bookingId, HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized"));
            }

            // Verify booking ownership
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            if (bookingOpt.isEmpty() || !bookingOpt.get().getCustomer().getId().equals(customer.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Booking not found or unauthorized"));
            }

            Payment payment = paymentService.createPaymentOrder(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("razorpayOrderId", payment.getRazorpayOrderId());
            response.put("razorpayKeyId", paymentService.getRazorpayKeyId());
            response.put("amount", payment.getAmount());
            response.put("currency", "INR");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Error creating payment order: " + e.getMessage()));
        }
    }

    /**
     * Verify and confirm payment
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request, HttpSession session) {
        try {
            User customer = (User) session.getAttribute("loggedInUser");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized"));
            }

            // Verify Razorpay signature (in production, verify cryptographic signature)
            // For now, we trust the payment was successful if we received the payment ID
            Payment payment = paymentService.handlePaymentSuccess(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId()
            );

            // Send confirmation email
            Booking booking = payment.getBooking();
            emailService.sendBookingConfirmationEmail(
                    booking.getCustomer().getEmail(),
                    booking.getCustomer().getName(),
                    booking.getService().getTitle(),
                    booking.getProvider().getName(),
                    booking.getBookingDateTime().toString(),
                    booking.getTotalAmount().toString()
            );

            // Send notification to provider
            emailService.sendProviderNotificationEmail(
                    booking.getProvider().getEmail(),
                    booking.getProvider().getName(),
                    booking.getCustomer().getName(),
                    booking.getService().getTitle(),
                    booking.getBookingDateTime().toString()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment successful! Confirmation email sent.");
            response.put("bookingId", booking.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Payment verification failed: " + e.getMessage()));
        }
    }

    /**
     * Handle payment failure
     * POST /api/payments/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<?> handlePaymentFailure(@RequestBody PaymentFailureRequest request) {
        try {
            paymentService.handlePaymentFailure(request.getRazorpayOrderId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Payment failed. Please try again.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Error handling payment failure: " + e.getMessage()));
        }
    }

    // Helper classes
    public static class PaymentVerificationRequest {
        private String razorpayOrderId;
        private String razorpayPaymentId;

        public String getRazorpayOrderId() { return razorpayOrderId; }
        public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

        public String getRazorpayPaymentId() { return razorpayPaymentId; }
        public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    }

    public static class PaymentFailureRequest {
        private String razorpayOrderId;

        public String getRazorpayOrderId() { return razorpayOrderId; }
        public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

