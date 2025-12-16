/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 */

package fin.controller;

import fin.entity.Company;
import fin.entity.User;
import fin.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    private User user1;
    private User user8;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");

        user8 = new User();
        user8.setId(8L);
        user8.setEmail("user8@example.com");
    }

    @Test
    void getCompaniesForUser_returnsCompaniesForAuthenticatedUser() throws Exception {
        Company c1 = new Company();
        c1.setId(10L);
        c1.setName("User1 Company");

        Company c8 = new Company();
        c8.setId(20L);
        c8.setName("User8 Company");

        when(companyService.getCompaniesForUser(eq(1L))).thenReturn(List.of(c1));
        when(companyService.getCompaniesForUser(eq(8L))).thenReturn(List.of(c8));

        // Call endpoint simulating request attribute 'user' with ID 1
        mockMvc.perform(get("/api/v1/companies/user")
                .requestAttr("user", user1)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(10));

        // Call endpoint simulating request attribute 'user' with ID 8
        mockMvc.perform(get("/api/v1/companies/user")
                .requestAttr("user", user8)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].id").value(20));
    }
}
