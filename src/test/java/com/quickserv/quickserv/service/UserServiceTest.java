package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Jane Doe");
        user.setEmail("Jane@example.com");
        user.setPassword("StrongPass1");
        user.setRole("customer");
        user.setPhone("+91 9876543210");
        user.setLocation("Pune");
    }

    @Test
    void registerUserEncryptsPasswordAndNormalizesFields() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertEquals("jane@example.com", saved.getEmail());
        assertEquals("CUSTOMER", saved.getRole());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals("encoded-password", captor.getValue().getPassword());
        assertEquals("+91 9876543210", saved.getPhone());
        assertEquals("Pune", saved.getLocation());
    }

    @Test
    void registerUserRejectsWeakPassword() {
        user.setPassword("weak");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.registerUser(user));

        assertTrue(exception.getMessage().contains("Password must be at least 8 characters"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserRejectsAdminSelfRegistration() {
        user.setRole("ADMIN");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.registerUser(user));

        assertTrue(exception.getMessage().contains("Invalid role selected"));
        verify(userRepository, never()).save(any(User.class));
    }
}
