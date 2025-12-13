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

package fin;

import fin.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot Application class for FIN Financial Management System.
 *
 * This is the entry point for the Spring Boot application that provides:
 * - REST API endpoints for all backend console functionalities
 * - Company management and fiscal periods
 * - Transaction processing and classification
 * - Financial reporting (trial balance, income statement, balance sheet, etc.)
 * - Budget management and variance analysis
 * - Payroll processing with SARS compliance
 * - Data management and audit trails
 * - Account management and depreciation
 * - System logs and time tracking
 *
 * The application supports multiple runtime modes:
 * - API Server: REST endpoints on port 8080
 * - Console: Interactive menu system
 * - Batch: Automated processing
 */
@SpringBootApplication
@ComponentScan(basePackages = {"fin"})
public class FinApplication {

    /**
     * Main method - Spring Boot application entry point
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FinApplication.class, args);
    }

    /**
     * Initialize default plans on application startup
     */
    @Bean
    public CommandLineRunner initializePlans(PlanService planService) {
        return args -> {
            planService.initializeDefaultPlans();
        };
    }
}