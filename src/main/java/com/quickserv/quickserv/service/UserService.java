package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admin@quickserve.com}")
    private String adminEmail;

    @Value("${admin.default.password:Admin@123}")
    private String adminDefaultPassword;

    // Constructor - Spring automatically provides both dependencies
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
        // Prevent self-registration as ADMIN
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Registration as ADMIN is not allowed.");
        }

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }

        // Encrypt password
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);

        // Save user (role is already set from form)
        User savedUser = userRepository.save(user);

        // Debug: print saved role
        System.out.println("Saved user with role: " + savedUser.getRole());

        return savedUser;
    }

    /**
     * Seeds the default admin account on startup if it does not already exist.
     * Credentials are read from application properties (admin.default.email /
     * admin.default.password). Change the password via those properties before
     * deploying to production.
     */
    public void ensureAdminExists() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User("Admin", adminEmail,
                    passwordEncoder.encode(adminDefaultPassword), "ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Default admin account created: " + adminEmail);
            System.out.println("⚠️  Change the admin password before deploying to production!");
        }
    }
    public boolean login(String email, String password) {
        // Find user by email
        User user = userRepository.findByEmail(email).orElse(null);

        // If user not found
        if (user == null) {
            return false;
        }

        // Compare plain password with encrypted one
        return passwordEncoder.matches(password, user.getPassword());
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}