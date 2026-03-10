package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final Pattern PASSWORD_POLICY =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)[A-Za-z\\d@_]{8,}$");
    private static final String PASSWORD_POLICY_MESSAGE =
            "Password must be at least 8 characters and include uppercase, lowercase, and a digit. Only @ and _ are allowed as special characters.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.reset-password.base-url:http://localhost:8080}")
    private String resetBaseUrl;

    @Value("${app.reset-password.expiry-minutes:30}")
    private long resetTokenExpiryMinutes;

    @Value("${app.reset-password.from:}")
    private String mailFrom;

    // Constructor - Spring automatically provides dependencies.
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public User registerUser(User user) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
        String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        String location = user.getLocation() == null ? "" : user.getLocation().trim();
        String serviceType = user.getServiceType() == null ? "" : user.getServiceType().trim();
        String password = user.getPassword() == null ? "" : user.getPassword();

        if (email.isEmpty()) {
            throw new RuntimeException("Email is required.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }
        if (!"CUSTOMER".equals(role) && !"PROVIDER".equals(role)) {
            throw new RuntimeException("Please select a valid role.");
        }
        if (location.isEmpty()) {
            throw new RuntimeException("Location is required.");
        }
        validatePasswordPolicy(password);
        if ("PROVIDER".equals(role) && serviceType.isEmpty()) {
            throw new RuntimeException("Please select the service type you provide.");
        }

        user.setEmail(email);
        user.setRole(role);
        user.setLocation(location);
        user.setServiceType("PROVIDER".equals(role) ? serviceType : null);
        if ("PROVIDER".equals(role)) {
            user.setServiceArea(location);
            user.setAvailabilityDays("Mon-Sat");
            user.setAvailabilityTimeSlots("09:00 AM - 07:00 PM");
        }

        String encryptedPassword = passwordEncoder.encode(password);
        user.setPassword(encryptedPassword);

        User savedUser = userRepository.save(user);
        System.out.println("Saved user with role: " + savedUser.getRole());

        return savedUser;
    }

    public boolean login(String email, String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void generateAndSendPasswordResetToken(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isEmpty()) {
            return;
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes);

        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        try {
            sendResetPasswordEmail(user.getEmail(), token);
            logger.info("Password reset email sent successfully to: {}", normalizedEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", normalizedEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send reset email. Please check mail configuration.", e);
        }
    }

    public boolean isResetTokenValid(String token) {
        User user = findByResetToken(token);
        return user != null && user.getResetTokenExpiry() != null && user.getResetTokenExpiry().isAfter(LocalDateTime.now());
    }

    public void resetPassword(String token, String newPassword) {
        User user = findByResetToken(token);
        if (user == null || user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset link is invalid or expired.");
        }

        validatePasswordPolicy(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public User findById(Long id) {
        return id == null ? null : userRepository.findById(id).orElse(null);
    }

    public User updateProviderProfile(Long userId, User profileInput) {
        User existingUser = findById(userId);
        if (existingUser == null || !"PROVIDER".equalsIgnoreCase(existingUser.getRole())) {
            throw new RuntimeException("Provider profile not found.");
        }

        String name = profileInput.getName() == null ? "" : profileInput.getName().trim();
        String location = profileInput.getLocation() == null ? "" : profileInput.getLocation().trim();
        String serviceType = profileInput.getServiceType() == null ? "" : profileInput.getServiceType().trim();

        if (name.isEmpty()) {
            throw new RuntimeException("Name is required.");
        }
        if (location.isEmpty()) {
            throw new RuntimeException("Location is required.");
        }
        if (serviceType.isEmpty()) {
            throw new RuntimeException("Please select your primary service type.");
        }

        existingUser.setName(name);
        existingUser.setLocation(location);
        existingUser.setServiceType(serviceType);
        existingUser.setProfilePhotoUrl(trimToNull(profileInput.getProfilePhotoUrl()));
        existingUser.setProviderDescription(trimToNull(profileInput.getProviderDescription()));
        existingUser.setExperienceYears(profileInput.getExperienceYears());
        existingUser.setServiceArea(trimToNull(profileInput.getServiceArea()));
        existingUser.setAvailabilityDays(trimToNull(profileInput.getAvailabilityDays()));
        existingUser.setAvailabilityTimeSlots(trimToNull(profileInput.getAvailabilityTimeSlots()));

        return userRepository.save(existingUser);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User findByResetToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return userRepository.findByResetToken(token).orElse(null);
    }

    private void validatePasswordPolicy(String password) {
        String value = password == null ? "" : password;
        if (!PASSWORD_POLICY.matcher(value).matches()) {
            throw new RuntimeException(PASSWORD_POLICY_MESSAGE);
        }
    }

    private void sendResetPasswordEmail(String toEmail, String token) {
        logger.info("Attempting to send reset email. Host: {}, Port: {}, From: {}",
            System.getenv("MAIL_HOST"), System.getenv("MAIL_PORT"), mailFrom);

        String resetLink = resetBaseUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();

        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(toEmail);
        message.setSubject("QuickServ Password Reset");
        message.setText(
                "We received a request to reset your QuickServ password.\n\n"
                        + "Use this link to set a new password:\n"
                        + resetLink + "\n\n"
                        + "This link expires in " + resetTokenExpiryMinutes + " minutes.\n"
                        + "If you did not request this, you can ignore this email."
        );

        mailSender.send(message);
    }
}

