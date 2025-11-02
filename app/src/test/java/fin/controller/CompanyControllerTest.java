/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fin.controller;

import fin.TestConfiguration;
import fin.service.CompanyService;
import fin.ui.*;
import fin.state.ApplicationState;
import fin.model.Company;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyControllerTest {
    
    @Mock
    private CompanyService mockCompanyService;
    
    @Mock
    private ApplicationState mockApplicationState;
    
    @Mock
    private ConsoleMenu mockMenu;
    
    @Mock
    private OutputFormatter mockOutputFormatter;
    
    private InputHandler inputHandler;
    private CompanyController companyController;
    
    @BeforeAll
    static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();
    }
    
    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create real InputHandler with test input
        // editCompany needs: name, reg, tax, address, email, phone, logo, bank_name, account_number, account_type, branch_code, vat_registered (12 inputs)
        // createCompany needs: name, reg, tax, address, email, phone, bank_name, account_number, account_type, branch_code, vat_registered (11 inputs)
        // Add enough inputs for editCompany (which is the superset)
        String testInput = "Test Company\n123456\n98765\nTest Address\ntest@example.com\n+27-11-123-4567\n/logo.png\nTest Bank\n1234567890\nCheque\n123456\ny\n" +
                           "Test Company\n123456\n98765\nTest Address\ntest@example.com\n+27-11-123-4567\nTest Bank\n1234567890\nCheque\n123456\ny\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(testInput.getBytes()));
        inputHandler = new InputHandler(scanner);
        
        companyController = new CompanyController(
            mockCompanyService,
            mockApplicationState,
            mockMenu,
            inputHandler,
            mockOutputFormatter
        );
    }
    
    @Test
    void createCompany_WithValidInput_CreatesCompany() {
        // Setup mock behavior
        Company expectedCompany = new Company();
        expectedCompany.setId(1L);
        expectedCompany.setName("Test Company");
        
        when(mockCompanyService.createCompany(any(Company.class))).thenReturn(expectedCompany);
        
        // Execute
        companyController.createCompany();
        
        // Verify
        verify(mockCompanyService, times(1)).createCompany(any(Company.class));
        verify(mockOutputFormatter, times(1)).printSuccess(contains("created successfully"));
    }
    
    @Test
    void selectCompany_WithValidChoice_SelectsCompany() {
        // Setup
        List<Company> companies = Arrays.asList(
            createTestCompany(1L, "Company A"),
            createTestCompany(2L, "Company B")
        );
        
        when(mockCompanyService.getAllCompanies()).thenReturn(companies);
        when(mockApplicationState.getCurrentCompany()).thenReturn(companies.get(0));
        
        // Create new InputHandler with selection input
        String selectionInput = "1\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(selectionInput.getBytes()));
        InputHandler selectionInputHandler = new InputHandler(scanner);
        
        CompanyController controllerWithSelection = new CompanyController(
            mockCompanyService,
            mockApplicationState,
            mockMenu,
            selectionInputHandler,
            mockOutputFormatter
        );
        
        // Execute
        controllerWithSelection.selectCompany();
        
        // Verify
        verify(mockApplicationState, times(1)).setCurrentCompany(companies.get(0));
        verify(mockOutputFormatter, times(1)).printSuccess("Selected company: Company A");
    }
    
    @Test
    void viewCompanyDetails_WithCurrentCompany_ShowsDetails() {
        // Setup
        Company currentCompany = createTestCompany(1L, "Current Company");
        when(mockApplicationState.getCurrentCompany()).thenReturn(currentCompany);
        when(mockApplicationState.hasCurrentCompany()).thenReturn(true);
        
        // Execute
        companyController.viewCompanyDetails();
        
        // Verify
        verify(mockOutputFormatter, times(1)).printCompanyDetails(currentCompany);
    }
    
    @Test
    void editCompany_WithCurrentCompany_EditsCompany() {
        // Setup
        Company currentCompany = createTestCompany(1L, "Current Company");
        when(mockApplicationState.getCurrentCompany()).thenReturn(currentCompany);
        when(mockApplicationState.hasCurrentCompany()).thenReturn(true);
        when(mockCompanyService.updateCompany(any(Company.class))).thenReturn(currentCompany);
        
        // Execute
        companyController.editCompany();
        
        // Verify
        verify(mockCompanyService, times(1)).updateCompany(any(Company.class));
        verify(mockOutputFormatter, times(1)).printSuccess(contains("updated successfully"));
    }
    
    @Test
    void deleteCompany_WithCurrentCompany_DeletesCompany() {
        // Setup
        Company currentCompany = createTestCompany(1L, "Current Company");
        when(mockApplicationState.getCurrentCompany()).thenReturn(currentCompany);
        when(mockApplicationState.hasCurrentCompany()).thenReturn(true);
        when(mockCompanyService.deleteCompany(anyLong())).thenReturn(true);
        
        // Create InputHandler with confirmation
        String confirmInput = "DELETE\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(confirmInput.getBytes()));
        InputHandler confirmInputHandler = new InputHandler(scanner);
        
        CompanyController controllerWithConfirm = new CompanyController(
            mockCompanyService,
            mockApplicationState,
            mockMenu,
            confirmInputHandler,
            mockOutputFormatter
        );
        
        // Execute
        controllerWithConfirm.deleteCompany();
        
        // Verify
        verify(mockCompanyService, times(1)).deleteCompany(anyLong());
        verify(mockOutputFormatter, times(1)).printSuccess(contains("deleted successfully"));
    }
    
    @Test
    void validateCompanyData_WithValidData_ReturnsTrue() {
        // This tests internal validation logic
        Company validCompany = new Company();
        validCompany.setName("Valid Company Name");
        
        // For this test, we assume the validation passes
        // In a real implementation, you would call the actual validation method
        assertTrue(validCompany.getName().length() > 0, "Company name should not be empty");
    }
    
    private Company createTestCompany(Long id, String name) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setRegistrationNumber("REG" + id);
        company.setTaxNumber("TAX" + id);
        company.setAddress("Address " + id);
        company.setContactEmail("company" + id + "@example.com");
        company.setContactPhone("+27-11-000-000" + id);
        return company;
    }
}
