package com.quickserv.quickserv.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send booking confirmation email to customer
     */
    public void sendBookingConfirmationEmail(String customerEmail, String customerName,
                                            String serviceName, String providerName,
                                            String bookingDateTime, String totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customerEmail);
            helper.setSubject("✅ Booking Confirmed - QuickServ");

            String htmlContent = buildBookingConfirmationHtml(customerName, serviceName,
                                                            providerName, bookingDateTime, totalAmount);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Booking confirmation email sent to: {}", customerEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send booking confirmation email to: {}", customerEmail, e);
        }
    }

    /**
     * Send booking cancellation email
     */
    public void sendBookingCancellationEmail(String customerEmail, String customerName,
                                            String serviceName, String refundAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customerEmail);
            helper.setSubject("❌ Booking Cancelled - QuickServ");

            String htmlContent = buildCancellationHtml(customerName, serviceName, refundAmount);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Cancellation email sent to: {}", customerEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send cancellation email to: {}", customerEmail, e);
        }
    }

    /**
     * Send booking completion email
     */
    public void sendBookingCompletionEmail(String customerEmail, String customerName,
                                          String serviceName, String providerName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(customerEmail);
            helper.setSubject("✨ Service Completed - Please Rate Your Provider");

            String htmlContent = buildCompletionHtml(customerName, serviceName, providerName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Completion email sent to: {}", customerEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send completion email to: {}", customerEmail, e);
        }
    }

    /**
     * Send provider notification email
     */
    public void sendProviderNotificationEmail(String providerEmail, String providerName,
                                             String customerName, String serviceName,
                                             String bookingDateTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(providerEmail);
            helper.setSubject("📌 New Booking Request - QuickServ");

            String htmlContent = buildProviderNotificationHtml(providerName, customerName,
                                                              serviceName, bookingDateTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Provider notification sent to: {}", providerEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send provider notification to: {}", providerEmail, e);
        }
    }

    // HTML template builders
    private String buildBookingConfirmationHtml(String customerName, String serviceName,
                                               String providerName, String bookingDateTime,
                                               String totalAmount) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #5A189A;'>✅ Booking Confirmed!</h2>" +
                "<p>Hi <strong>" + customerName + "</strong>,</p>" +
                "<p>Your booking has been confirmed. Here are the details:</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Service:</strong> " + serviceName + "</p>" +
                "<p><strong>Provider:</strong> " + providerName + "</p>" +
                "<p><strong>Date & Time:</strong> " + bookingDateTime + "</p>" +
                "<p><strong>Total Amount:</strong> ₹" + totalAmount + "</p>" +
                "</div>" +
                "<p>The provider will contact you shortly to confirm the appointment.</p>" +
                "<p style='color: #666;'>Thank you for using QuickServ!</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildCancellationHtml(String customerName, String serviceName, String refundAmount) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #e74c3c;'>❌ Booking Cancelled</h2>" +
                "<p>Hi <strong>" + customerName + "</strong>,</p>" +
                "<p>Your booking for <strong>" + serviceName + "</strong> has been cancelled.</p>" +
                "<p style='background-color: #fff3cd; padding: 10px; border-radius: 5px; color: #856404;'>" +
                "Refund Amount: <strong>₹" + refundAmount + "</strong>" +
                "</p>" +
                "<p>The refund will be processed within 3-5 business days.</p>" +
                "<p style='color: #666;'>If you have any questions, please contact our support team.</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildCompletionHtml(String customerName, String serviceName, String providerName) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #27ae60;'>✨ Service Completed!</h2>" +
                "<p>Hi <strong>" + customerName + "</strong>,</p>" +
                "<p>Your service <strong>" + serviceName + "</strong> by <strong>" + providerName + "</strong> has been completed.</p>" +
                "<p style='background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "We'd love to hear from you! Please rate your experience and write a review." +
                "</p>" +
                "<p><a href='https://quickserv.com/my-bookings' style='background-color: #5A189A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Leave a Review</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildProviderNotificationHtml(String providerName, String customerName,
                                                 String serviceName, String bookingDateTime) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #5A189A;'>📌 New Booking Request</h2>" +
                "<p>Hi <strong>" + providerName + "</strong>,</p>" +
                "<p>You have received a new booking request:</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Customer:</strong> " + customerName + "</p>" +
                "<p><strong>Service:</strong> " + serviceName + "</p>" +
                "<p><strong>Requested Date & Time:</strong> " + bookingDateTime + "</p>" +
                "</div>" +
                "<p><a href='https://quickserv.com/provider/bookings' style='background-color: #5A189A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>View Booking</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}

