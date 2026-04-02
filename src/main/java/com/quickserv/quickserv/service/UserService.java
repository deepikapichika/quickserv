package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Booking;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.BookingRepository;
import com.quickserv.quickserv.repository.PaymentRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.ReviewRepository;
import com.quickserv.quickserv.repository.ServiceRepository;
import com.quickserv.quickserv.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final Pattern PHONE_POLICY = Pattern.compile("^[0-9+\\-()\\s]{7,20}$");
    private static final Set<String> ALLOWED_SELF_REGISTRATION_ROLES = Set.of("CUSTOMER", "PROVIDER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;

    // Constructor - Spring automatically provides all dependencies
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       BookingRepository bookingRepository,
                       PaymentRepository paymentRepository,
                       ReviewRepository reviewRepository,
                       ServiceRepository serviceRepository,
                       ProviderRepository providerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.serviceRepository = serviceRepository;
        this.providerRepository = providerRepository;
    }

    public User registerUser(User user) {
        String name = normalizeRequiredText(user.getName(), "Name");
        String email = normalizeEmail(user.getEmail());
        String phone = normalizePhone(user.getPhone());
        String location = normalizeRequiredText(user.getLocation(), "Location");
        String role = normalizeRole(user.getRole());

        validatePasswordPolicy(user.getPassword());

        if (!ALLOWED_SELF_REGISTRATION_ROLES.contains(role)) {
            throw new RuntimeException("Invalid role selected. Please choose CUSTOMER or PROVIDER.");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setLocation(location);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        System.out.println("Saved user with role: " + savedUser.getRole());
        return savedUser;
    }

    public boolean login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);

        if (user == null) {
            return false;
        }

        return passwordEncoder.matches(password, user.getPassword());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email)).orElse(null);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateCustomerProfile(Long userId,
                                      String name,
                                      String phone,
                                      String address,
                                      String profilePhotoUrl,
                                      java.math.BigDecimal latitude,
                                      java.math.BigDecimal longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(normalizePhone(phone));
        }
        if (address != null && !address.trim().isEmpty()) {
            user.setLocation(address.trim());
        }
        if (profilePhotoUrl != null) {
            user.setProfilePhotoUrl(profilePhotoUrl.trim().isEmpty() ? null : profilePhotoUrl.trim());
        }
        user.setLatitude(latitude);
        user.setLongitude(longitude);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if ("ADMIN".equalsIgnoreCase(targetUser.getRole())) {
            throw new RuntimeException("Admin users cannot be deleted.");
        }

        // Remove booking-linked rows first to satisfy foreign key constraints.
        List<Booking> relatedBookings = bookingRepository.findByCustomerOrProvider(targetUser, targetUser);
        List<Long> bookingIds = relatedBookings.stream().map(Booking::getId).toList();

        if (!bookingIds.isEmpty()) {
            paymentRepository.deleteByBookingIdIn(bookingIds);
            reviewRepository.deleteByBookingIdIn(bookingIds);
            bookingRepository.deleteByCustomerOrProvider(targetUser, targetUser);
        }

        // Remove direct user-owned rows.
        reviewRepository.deleteByCustomerOrProvider(targetUser, targetUser);
        serviceRepository.deleteByProvider(targetUser);
        providerRepository.deleteByUser(targetUser);

        userRepository.delete(targetUser);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || !PASSWORD_POLICY.matcher(password).matches()) {
            throw new RuntimeException("Password must be at least 8 characters and include uppercase, lowercase, and a number.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new RuntimeException("Role is required.");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Phone number is required.");
        }
        String cleanPhone = phone.trim();
        if (!PHONE_POLICY.matcher(cleanPhone).matches()) {
            throw new RuntimeException("Please enter a valid phone number.");
        }
        return cleanPhone;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required.");
        }
        return value.trim();
    }
}