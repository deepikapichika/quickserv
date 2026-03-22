package com.quickserv.quickserv.service;

import com.quickserv.quickserv.dto.provider.ProviderCreateRequest;
import com.quickserv.quickserv.dto.provider.ProviderResponse;
import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.Provider;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.exception.BusinessValidationException;
import com.quickserv.quickserv.exception.ResourceNotFoundException;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import com.quickserv.quickserv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ServiceService serviceService;

    @InjectMocks
    private ProviderService providerService;

    @Test
    void addProviderRejectsNonProviderRole() {
        User customer = new User();
        ReflectionTestUtils.setField(customer, "id", 11L);
        customer.setRole("CUSTOMER");

        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setUserId(11L);
        request.setCategoryId(5L);

        when(userRepository.findById(11L)).thenReturn(Optional.of(customer));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> providerService.addProvider(request));

        assertEquals("Only users with role PROVIDER can register as providers.", ex.getMessage());
    }

    @Test
    void addProviderCreatesProviderForValidProviderUser() {
        User providerUser = new User();
        ReflectionTestUtils.setField(providerUser, "id", 21L);
        providerUser.setName("Priya");
        providerUser.setEmail("priya@example.com");
        providerUser.setRole("PROVIDER");
        providerUser.setLocation("Mumbai");

        Category category = new Category();
        category.setId(3L);
        category.setName("Electrician");

        ProviderCreateRequest request = new ProviderCreateRequest();
        request.setUserId(21L);
        request.setCategoryId(3L);
        request.setServiceCharge(new BigDecimal("450.00"));
        request.setAvailability("Mon-Fri");

        when(userRepository.findById(21L)).thenReturn(Optional.of(providerUser));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(providerRepository.existsByUser(providerUser)).thenReturn(false);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> {
            Provider saved = invocation.getArgument(0);
            saved.setProviderId(100L);
            return saved;
        });

        ProviderResponse response = providerService.addProvider(request);

        assertEquals(100L, response.getProviderId());
        assertEquals(21L, response.getUserId());
        assertEquals(3L, response.getCategoryId());
        assertEquals("Mumbai", response.getUserLocation());
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    void getProviderResponsesByCategoryThrowsWhenCategoryMissing() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> providerService.getProviderResponsesByCategory(999L));

        assertEquals("Category not found for id: 999", ex.getMessage());
    }
}
