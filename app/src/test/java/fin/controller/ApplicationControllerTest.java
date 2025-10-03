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
import fin.ui.*;
import fin.state.ApplicationState;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationControllerTest {
    
    @Mock
    private ConsoleMenu mockMenu;
    
    @Mock
    private OutputFormatter mockOutputFormatter;
    
    @Mock
    private ApplicationState mockApplicationState;
    
    @Mock
    private CompanyController mockCompanyController;
    
    @Mock
    private FiscalPeriodController mockFiscalPeriodController;
    
    @Mock
    private ImportController mockImportController;
    
    @Mock
    private ReportController mockReportController;
    
    @Mock
    private DataManagementController mockDataManagementController;
    
    @Mock
    private PayrollController mockPayrollController;
    
    private InputHandler inputHandler;
    private ApplicationController applicationController;
    
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
        
        // Setup all controller mocks to prevent infinite loops
        doNothing().when(mockCompanyController).handleCompanySetup();
        doNothing().when(mockDataManagementController).handleDataManagement();
        doNothing().when(mockPayrollController).handlePayrollManagement(anyLong());
        
        // Stub applicationState methods to avoid NPE
        when(mockApplicationState.hasCurrentCompany()).thenReturn(false);
        when(mockApplicationState.hasCurrentFiscalPeriod()).thenReturn(false);
        
        // Create InputHandler with exit command
        String exitInput = "12\ny\n"; // Exit is option 12, confirm with y
        Scanner scanner = new Scanner(new ByteArrayInputStream(exitInput.getBytes()));
        inputHandler = new InputHandler(scanner);

        applicationController = new ApplicationController(
            mockMenu,
            inputHandler,
            mockOutputFormatter,
            mockApplicationState,
            mockCompanyController,
            mockFiscalPeriodController,
            mockImportController,
            mockReportController,
            mockDataManagementController,
            mockPayrollController
        );
    }    @Test
    void start_DisplaysMainMenu() {
        // Execute
        applicationController.start();
        
        // Verify main menu is displayed
        verify(mockMenu, atLeastOnce()).displayMainMenu();
    }
    
    @Test
    void start_WithCompanySetupChoice_CallsCompanyController() {
        // Setup mock to do nothing when handleCompanySetup is called
        doNothing().when(mockCompanyController).handleCompanySetup();
        
        // Setup input for company setup choice then exit
        String companySetupInput = "1\n12\ny\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(companySetupInput.getBytes()));
        InputHandler setupInputHandler = new InputHandler(scanner);

        ApplicationController controllerWithSetup = new ApplicationController(
            mockMenu,
            setupInputHandler,
            mockOutputFormatter,
            mockApplicationState,
            mockCompanyController,
            mockFiscalPeriodController,
            mockImportController,
            mockReportController,
            mockDataManagementController,
            mockPayrollController
        );
        
        // Execute
        controllerWithSetup.start();
        
        // Verify
        verify(mockCompanyController, times(1)).handleCompanySetup();
    }
    
    @Test
    void start_WithDataManagementChoice_CallsDataManagementController() {
        // Setup mock to do nothing when handleDataManagement is called
        doNothing().when(mockDataManagementController).handleDataManagement();
        
        // Setup input for data management choice then exit
        String dataManagementInput = "7\n12\ny\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(dataManagementInput.getBytes()));
        InputHandler dataInputHandler = new InputHandler(scanner);

        ApplicationController controllerWithData = new ApplicationController(
            mockMenu,
            dataInputHandler,
            mockOutputFormatter,
            mockApplicationState,
            mockCompanyController,
            mockFiscalPeriodController,
            mockImportController,
            mockReportController,
            mockDataManagementController,
            mockPayrollController
        );
        
        // Execute
        controllerWithData.start();
        
        // Verify
        verify(mockDataManagementController, times(1)).handleDataManagement();
    }
    
    @Test
    void start_WithExitChoice_ExitsCleanly() {
        // Execute (already set up with exit input)
        applicationController.start();
        
        // Verify menu was displayed at least once
        verify(mockMenu, atLeastOnce()).displayMainMenu();
        // Should complete without throwing exceptions
        assertTrue(true, "Application should exit cleanly");
    }
    
    @Test
    void applicationControllerCreation_WithAllDependencies_CreatesSuccessfully() {
        // Test that all dependencies are properly injected
        assertNotNull(applicationController, "ApplicationController should be created successfully");
    }
}
