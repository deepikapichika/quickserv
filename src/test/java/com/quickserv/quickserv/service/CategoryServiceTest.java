package com.quickserv.quickserv.service;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.repository.CategoryRepository;
import com.quickserv.quickserv.repository.ProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllCategoriesReturnsAlphabeticalList() {
        Category electrician = new Category();
        electrician.setName("Electrician");
        Category plumber = new Category();
        plumber.setName("Plumber");

        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(electrician, plumber));

        List<Category> categories = categoryService.getAllCategories();

        assertEquals(2, categories.size());
        assertEquals("Electrician", categories.get(0).getName());
        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    void createCategoryTrimsAndSavesUniqueName() {
        when(categoryRepository.existsByNameIgnoreCase("Home Cleaning")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = categoryService.createCategory("  Home Cleaning  ", "  Deep cleaning services  ");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("Home Cleaning", saved.getName());
        assertEquals("Deep cleaning services", captor.getValue().getDescription());
    }

    @Test
    void createCategoryRejectsBlankName() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.createCategory("   ", "Anything"));

        assertEquals("Category name must not be empty.", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(categoryRepository.existsByNameIgnoreCase("Plumber")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.createCategory("Plumber", "Duplicate"));

        assertEquals("Category name must be unique.", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategoryRejectsAssignedCategories() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Electrician");

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(providerRepository.existsByCategory(category)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.deleteCategory(5L));

        assertEquals("Cannot delete category because it is currently assigned to providers.", exception.getMessage());
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}

