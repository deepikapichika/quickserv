package com.quickserv.quickserv.controller;

import com.quickserv.quickserv.dto.provider.ProviderResponse;
import com.quickserv.quickserv.entity.User;
import com.quickserv.quickserv.service.CategoryService;
import com.quickserv.quickserv.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock
    private ProviderService providerService;

    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProviderController controller = new ProviderController(providerService, categoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void addProviderRequiresLogin() throws Exception {
        String payload = "{\"userId\":1,\"categoryId\":2}";

        mockMvc.perform(post("/provider/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void byCategoryReturnsDtoList() throws Exception {
        ProviderResponse response = new ProviderResponse();
        response.setProviderId(44L);
        response.setUserName("Raj");
        response.setCategoryName("Plumber");

        when(providerService.getProviderResponsesByCategory(2L)).thenReturn(List.of(response));

        mockMvc.perform(get("/provider/byCategory/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(44))
                .andExpect(jsonPath("$[0].categoryName").value("Plumber"));
    }

    @Test
    void addProviderAllowsAdminForAnyUser() throws Exception {
        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setRole("ADMIN");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);

        ProviderResponse response = new ProviderResponse();
        response.setProviderId(9L);
        response.setUserId(22L);

        when(providerService.addProvider(any())).thenReturn(response);

        String payload = "{\"userId\":22,\"categoryId\":3,\"serviceCharge\":500}";

        mockMvc.perform(post("/provider/add")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.provider.providerId").value(9));
    }
}
