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
package fin.context;

import fin.TestConfiguration;
import fin.controller.ApplicationController;
import fin.controller.CompanyController;
import fin.service.CompanyService;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;
import fin.state.ApplicationState;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationContextTest {
    
    private ApplicationContext applicationContext;
    
    @BeforeAll
    static void setUpClass() throws Exception {
        // TestDatabaseConfig will read TEST_DATABASE_* environment variables
        TestConfiguration.setupTestDatabase();
        // Set test database URL for ApplicationContext dependency injection
        System.setProperty("fin.database.test.url", TestConfiguration.TEST_DB_URL);
    }
    
    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
        System.clearProperty("fin.database.test.url");
    }
    
    @BeforeEach
    void setUp() {
        applicationContext = new ApplicationContext();
    }
    
    @Test
    void get_ApplicationController_ReturnsNonNullInstance() {
        ApplicationController controller = applicationContext.getApplicationController();
        
        assertNotNull(controller, "ApplicationController should not be null");
    }
    
    @Test
    void get_CompanyService_ReturnsNonNullInstance() {
        CompanyService service = applicationContext.get(CompanyService.class);
        
        assertNotNull(service, "CompanyService should not be null");
    }
    
    @Test
    void get_CompanyController_ReturnsNonNullInstance() {
        CompanyController controller = applicationContext.get(CompanyController.class);
        
        assertNotNull(controller, "CompanyController should not be null");
    }
    
    @Test
    void get_UIComponents_ReturnNonNullInstances() {
        ConsoleMenu menu = applicationContext.get(ConsoleMenu.class);
        InputHandler inputHandler = applicationContext.get(InputHandler.class);
        OutputFormatter outputFormatter = applicationContext.get(OutputFormatter.class);
        
        assertNotNull(menu, "ConsoleMenu should not be null");
        assertNotNull(inputHandler, "InputHandler should not be null");
        assertNotNull(outputFormatter, "OutputFormatter should not be null");
    }
    
    @Test
    void get_ApplicationState_ReturnsNonNullInstance() {
        ApplicationState state = applicationContext.get(ApplicationState.class);
        
        assertNotNull(state, "ApplicationState should not be null");
    }
    
    @Test
    void get_SameService_ReturnsSameInstance() {
        CompanyService service1 = applicationContext.get(CompanyService.class);
        CompanyService service2 = applicationContext.get(CompanyService.class);
        
        assertSame(service1, service2, "Should return the same singleton instance");
    }
    
    @Test
    void applicationContextCreation_WithTestDatabase_CreatesSuccessfully() {
        assertNotNull(applicationContext, "ApplicationContext should be created successfully");
        assertDoesNotThrow(() -> {
            applicationContext.getApplicationController();
        }, "Should create ApplicationController without throwing exceptions");
    }
    
    @Test
    void allServices_AreProperlyWired() {
        // Test that all core services can be retrieved without errors
        assertDoesNotThrow(() -> {
            applicationContext.get(CompanyService.class);
            applicationContext.get(ConsoleMenu.class);
            applicationContext.get(ApplicationState.class);
        }, "All services should be properly wired and accessible");
    }
}
