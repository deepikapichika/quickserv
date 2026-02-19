package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor - Spring automatically provides both dependencies
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
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