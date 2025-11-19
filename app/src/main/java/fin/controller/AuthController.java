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

import fin.model.Plan;
import fin.model.User;
import fin.repository.PlanRepository;
import fin.service.UserService;
import fin.state.ApplicationState;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.util.List;

/**
 * Authentication controller for console-based user registration and login
 * Handles the authentication flow before main application access
 */
public class AuthController {
    private final UserService userService;
    private final PlanRepository planRepository;
    private final ApplicationState applicationState;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;

    // Menu option constants
    private static final int MENU_OPTION_LOGIN = 1;
    private static final int MENU_OPTION_REGISTER = 2;
    private static final int MENU_OPTION_EXIT = 3;
    private static final int AUTH_MENU_MAX_OPTION = 3;

    public AuthController(
        UserService userService,
        PlanRepository planRepository,
        ApplicationState applicationState,
        InputHandler inputHandler,
        OutputFormatter outputFormatter
    ) {
        this.userService = userService;
        this.planRepository = planRepository;
        this.applicationState = applicationState;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }

    /**
     * Main authentication loop - handles login/registration until successful authentication
     */
    public boolean authenticateUser() {
        outputFormatter.printHeader("FIN Authentication");
        outputFormatter.printInfo("Please login or register to access the FIN system");

        boolean authenticated = false;
        boolean exitRequested = false;

        while (!authenticated && !exitRequested) {
            try {
                displayAuthMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, AUTH_MENU_MAX_OPTION);

                switch (choice) {
                    case MENU_OPTION_LOGIN:
                        authenticated = handleLogin();
                        break;
                    case MENU_OPTION_REGISTER:
                        authenticated = handleRegistration();
                        break;
                    case MENU_OPTION_EXIT:
                        exitRequested = handleExit();
                        break;
                    default:
                        outputFormatter.printError("Invalid choice. Please try again.");
                }

            } catch (Exception e) {
                outputFormatter.printError("Authentication error: " + e.getMessage());
                outputFormatter.printInfo("Please try again or contact support if the problem persists.");
            }
        }

        if (authenticated) {
            displayWelcomeMessage();
        }

        return authenticated;
    }

    /**
     * Display authentication menu
     */
    private void displayAuthMenu() {
        outputFormatter.printSeparator();
        outputFormatter.printPlain("Authentication Menu:");
        outputFormatter.printPlain("1. Login");
        outputFormatter.printPlain("2. Register New Account");
        outputFormatter.printPlain("3. Exit");
        outputFormatter.printSeparator();
    }

    /**
     * Handle user login
     */
    private boolean handleLogin() {
        outputFormatter.printHeader("User Login");

        try {
            String email = inputHandler.getString("Email");
            String password = inputHandler.getString("Password");

            User user = userService.authenticateUser(email, password);

            // Set authenticated user in application state
            applicationState.setCurrentUser(user);

            outputFormatter.printSuccess("Login successful!");
            outputFormatter.printInfo("Welcome back, " + user.getFirstName() + " " + user.getLastName());

            return true;

        } catch (IllegalArgumentException e) {
            outputFormatter.printError("Login failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            outputFormatter.printError("Login error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle user registration
     */
    private boolean handleRegistration() {
        outputFormatter.printHeader("User Registration");

        try {
            // Get user details
            String email = inputHandler.getString("Email");
            String password = inputHandler.getString("Password");
            String confirmPassword = inputHandler.getString("Confirm Password");
            String firstName = inputHandler.getString("First Name");
            String lastName = inputHandler.getString("Last Name");

            // Validate password confirmation
            if (!password.equals(confirmPassword)) {
                outputFormatter.printError("Passwords do not match. Please try again.");
                return false;
            }

            // Display available plans
            Long planId = selectPlan();

            // Register user
            User newUser = userService.registerUser(email, password, firstName, lastName, planId);

            // Set authenticated user in application state
            applicationState.setCurrentUser(newUser);

            outputFormatter.printSuccess("Registration successful!");
            outputFormatter.printInfo("Welcome to FIN, " + newUser.getFirstName() + " " + newUser.getLastName());

            return true;

        } catch (IllegalArgumentException e) {
            outputFormatter.printError("Registration failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            outputFormatter.printError("Registration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Display available plans and let user select one
     */
    private Long selectPlan() {
        outputFormatter.printSubHeader("Select a Plan");

        try {
            // Get available plans from repository
            List<Plan> plans = planRepository.findAllActive();

            if (plans.isEmpty()) {
                outputFormatter.printWarning("No plans available. Using default plan.");
                return 1L; // Default to plan ID 1
            }

            outputFormatter.printInfo("Available plans:");
            for (int i = 0; i < plans.size(); i++) {
                Plan plan = plans.get(i);
                outputFormatter.printPlain((i + 1) + ". " + plan.getName() +
                    " - " + plan.getPrice() + " " + plan.getCurrency() + "/month");
                outputFormatter.printPlain("   " + plan.getDescription());
            }

            int planChoice = inputHandler.getInteger("Select a plan", 1, plans.size());
            return plans.get(planChoice - 1).getId();

        } catch (Exception e) {
            outputFormatter.printWarning("Could not load plans. Using default plan.");
            return 1L; // Default to plan ID 1
        }
    }

    /**
     * Handle exit request
     */
    private boolean handleExit() {
        outputFormatter.printHeader("Exit Application");

        boolean confirmExit = inputHandler.getBoolean("Are you sure you want to exit the FIN application?");

        if (confirmExit) {
            outputFormatter.printInfo("Thank you for using FIN. Goodbye!");
            return true;
        } else {
            outputFormatter.printInfo("Returning to authentication menu");
            return false;
        }
    }

    /**
     * Display welcome message after successful authentication
     */
    private void displayWelcomeMessage() {
        outputFormatter.printSeparator();
        outputFormatter.printSuccess("Authentication successful!");
        outputFormatter.printInfo("You are now logged in as: " + applicationState.getCurrentUserDisplayName());
        outputFormatter.printSeparator();

        inputHandler.waitForEnter("Press Enter to continue to the main application");
    }

    /**
     * Logout current user
     */
    public void logout() {
        User currentUser = applicationState.getCurrentUser();
        if (currentUser != null) {
            outputFormatter.printInfo("Logging out user: " + currentUser.getEmail());
        }

        applicationState.setCurrentUser(null);
        outputFormatter.printSuccess("Successfully logged out");
    }

    /**
     * Check if user is currently authenticated
     */
    public boolean isAuthenticated() {
        return applicationState.isAuthenticated();
    }

    /**
     * Get current authenticated user
     */
    public User getCurrentUser() {
        return applicationState.getCurrentUser();
    }
}