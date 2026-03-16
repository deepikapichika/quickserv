package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.entity.Category;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;
    private MockHttpSession adminSession;
    private MockHttpSession providerSession;

    @BeforeEach
    void setUp() {
        CategoryController controller = new CategoryController(categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        User admin = new User();
        admin.setRole("ADMIN");
        admin.setName("Admin User");
        adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInUser", admin);

        User provider = new User();
        provider.setRole("PROVIDER");
        provider.setName("Provider User");
        providerSession = new MockHttpSession();
        providerSession.setAttribute("loggedInUser", provider);
    }

    @Test
    void getAllCategoriesReturnsPayload() throws Exception {
        Category category = new Category();
        category.setId(10L);
        category.setName("Electrician");
        category.setDescription("Electrical services");
        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/category/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Electrician"));
    }

    @Test
    void addCategoryRejectsNonAdmin() throws Exception {
        mockMvc.perform(post("/category/add")
                        .session(providerSession)
                        .param("category_name", "Painting"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addCategoryAllowsAdmin() throws Exception {
        Category saved = new Category();
        saved.setId(7L);
        saved.setName("Painting");
        saved.setDescription("Wall painting");
        when(categoryService.createCategory(eq("Painting"), eq("Wall painting"))).thenReturn(saved);

        mockMvc.perform(post("/category/add")
                        .session(adminSession)
                        .param("category_name", "Painting")
                        .param("description", "Wall painting"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.category.name").value("Painting"));
    }

    @Test
    void deleteCategoryAllowsAdminAndDelegates() throws Exception {
        mockMvc.perform(delete("/category/9").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(categoryService).deleteCategory(9L);
    }
}

