package com.quickserv.quickserv.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.Payment;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.BookingRepository;
import com.quickserv.quickserv.repository.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Create a Razorpay order for booking payment
     */
    @Transactional
    public Payment createPaymentOrder(Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            if (booking.getTotalAmount() == null || booking.getTotalAmount().signum() <= 0) {
                throw new BusinessValidationException("Invalid booking amount");
            }

            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", booking.getTotalAmount().multiply(new BigDecimal("100")).longValue()); // Convert to paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + bookingId);
            orderRequest.put("notes", new JSONObject()
                    .put("customer_name", booking.getCustomer().getName())
                    .put("service_name", booking.getService().getTitle())
                    .put("booking_id", bookingId));

            Order order = client.orders.create(orderRequest);
            String razorpayOrderId = (String) order.get("id");

            // Save payment record
            Payment payment = new Payment(booking, razorpayOrderId, booking.getTotalAmount());
            paymentRepository.save(payment);

            logger.info("Razorpay order created: {} for booking: {}", razorpayOrderId, bookingId);
            return payment;

        } catch (Exception e) {
            logger.error("Failed to create Razorpay order for booking: {}", bookingId, e);
            throw new BusinessValidationException("Failed to initiate payment: " + e.getMessage());
        }
    }

    /**
     * Handle successful payment
     */
    @Transactional
    public Payment handlePaymentSuccess(String razorpayOrderId, String razorpayPaymentId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        // Update booking status to CONFIRMED
        Booking booking = payment.getBooking();
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        logger.info("Payment successful: {} for booking: {}", razorpayPaymentId, booking.getId());
        return paymentRepository.save(payment);
    }

    /**
     * Handle failed payment
     */
    @Transactional
    public Payment handlePaymentFailure(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        logger.warn("Payment failed for booking: {}", payment.getBooking().getId());
        return paymentRepository.save(payment);
    }

    /**
     * Get payment by booking ID
     */
    public Optional<Payment> getPaymentByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    /**
     * Process refund (for cancelled bookings)
     */
    @Transactional
    public void processRefund(Long bookingId, BigDecimal refundAmount) {
        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                logger.info("Refund processed for booking: {}, amount: {}", bookingId, refundAmount);
                // In production, call Razorpay API to actually process the refund
            }
        }
    }

    /**
     * Get Razorpay Key ID (for frontend)
     */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
}


