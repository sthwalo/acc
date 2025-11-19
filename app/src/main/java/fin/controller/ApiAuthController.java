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

import com.google.gson.Gson;
import fin.api.dto.requests.LoginRequest;
import fin.api.dto.requests.RegisterRequest;
import fin.api.dto.responses.ApiResponse;
import fin.api.dto.responses.AuthResponse;
import fin.model.User;
import fin.state.ApplicationState;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * API-based Authentication controller for console-based user registration and login
 * Makes HTTP calls to the authentication API endpoints for testing purposes
 */
public class ApiAuthController {
    private final ApplicationState applicationState;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    private final Gson gson;
    private final String baseUrl;

    // Menu option constants
    private static final int MENU_OPTION_LOGIN = 1;
    private static final int MENU_OPTION_REGISTER = 2;
    private static final int MENU_OPTION_EXIT = 3;
    private static final int AUTH_MENU_MAX_OPTION = 3;

    // HTTP timeouts
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    public ApiAuthController(
        ApplicationState applicationState,
        InputHandler inputHandler,
        OutputFormatter outputFormatter,
        Gson gson,
        String baseUrl
    ) {
        this.applicationState = applicationState;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
        this.gson = gson;
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8080";
    }

    /**
     * Main authentication loop - handles login/registration until successful authentication
     */
    public boolean authenticateUser() {
        outputFormatter.printHeader("FIN API Authentication Test");
        outputFormatter.printInfo("Testing authentication API endpoints via console interface");
        outputFormatter.printInfo("API Base URL: " + baseUrl);

        boolean authenticated = false;
        boolean exitRequested = false;

        while (!authenticated && !exitRequested) {
            try {
                displayAuthMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, AUTH_MENU_MAX_OPTION);

                switch (choice) {
                    case MENU_OPTION_LOGIN:
                        authenticated = handleApiLogin();
                        break;
                    case MENU_OPTION_REGISTER:
                        authenticated = handleApiRegistration();
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
        outputFormatter.printPlain("API Authentication Test Menu:");
        outputFormatter.printPlain("1. Test Login API");
        outputFormatter.printPlain("2. Test Register API");
        outputFormatter.printPlain("3. Exit");
        outputFormatter.printSeparator();
    }

    /**
     * Handle API login test
     */
    private boolean handleApiLogin() {
        outputFormatter.printHeader("API Login Test");

        try {
            String email = inputHandler.getString("Email");
            String password = inputHandler.getString("Password");

            // Create login request
            LoginRequest loginRequest = new LoginRequest(email, password);

            // Make API call
            ApiResponse<AuthResponse> response = makeApiCall("/api/v1/auth/login", "POST", loginRequest, AuthResponse.class);

            if (response.isSuccess() && response.getData() != null) {
                // Create user object from API response
                AuthResponse authData = response.getData();
                User user = createUserFromAuthResponse(authData);

                // Set authenticated user in application state
                applicationState.setCurrentUser(user);

                outputFormatter.printSuccess("API Login successful!");
                outputFormatter.printInfo("Welcome back, " + user.getFirstName() + " " + user.getLastName());
                outputFormatter.printInfo("JWT Token received: " + authData.getToken().substring(0, 20) + "...");

                return true;
            } else {
                outputFormatter.printError("API Login failed: " + response.getMessage());
                return false;
            }

        } catch (Exception e) {
            outputFormatter.printError("API Login error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle API registration test
     */
    private boolean handleApiRegistration() {
        outputFormatter.printHeader("API Registration Test");

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

            // Use default plan ID for testing
            Long planId = 1L;
            outputFormatter.printInfo("Using default plan ID: " + planId);

            // Create registration request
            RegisterRequest registerRequest = new RegisterRequest(email, password, firstName, lastName, planId);

            // Make API call
            ApiResponse<AuthResponse> response = makeApiCall("/api/v1/auth/register", "POST", registerRequest, AuthResponse.class);

            if (response.isSuccess() && response.getData() != null) {
                // Create user object from API response
                AuthResponse authData = response.getData();
                User user = createUserFromAuthResponse(authData);

                // Set authenticated user in application state
                applicationState.setCurrentUser(user);

                outputFormatter.printSuccess("API Registration successful!");
                outputFormatter.printInfo("Welcome to FIN, " + user.getFirstName() + " " + user.getLastName());
                outputFormatter.printInfo("User registered and JWT token received: " + authData.getToken().substring(0, 20) + "...");

                return true;
            } else {
                outputFormatter.printError("API Registration failed: " + response.getMessage());
                return false;
            }

        } catch (Exception e) {
            outputFormatter.printError("API Registration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Make HTTP API call to authentication endpoint
     */
    private <T, R> ApiResponse<R> makeApiCall(String endpoint, String method, T requestBody, Class<R> responseType) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Configure connection
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            // Set request body for POST requests
            if (requestBody != null && ("POST".equals(method) || "PUT".equals(method))) {
                conn.setDoOutput(true);
                String jsonBody = gson.toJson(requestBody);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Get response
            int responseCode = conn.getResponseCode();

            // Read response body
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ?
                        conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }

            // Parse API response
            @SuppressWarnings("unchecked")
            ApiResponse<R> apiResponse = (ApiResponse<R>) gson.fromJson(responseBody.toString(), ApiResponse.class);

            // If response has data and we need to parse it as specific type
            if (apiResponse.getData() != null && responseType != null) {
                // Re-parse the data field as the specific type
                String dataJson = gson.toJson(apiResponse.getData());
                R typedData = gson.fromJson(dataJson, responseType);
                apiResponse.setData(typedData);
            }

            return apiResponse;

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Create User object from AuthResponse
     */
    private User createUserFromAuthResponse(AuthResponse authResponse) {
        AuthResponse.UserInfo userInfo = authResponse.getUser();
        User user = new User();
        user.setId(userInfo.getId());
        user.setEmail(userInfo.getEmail());
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(userInfo.getLastName());
        // Set JWT token for API calls
        user.setToken(authResponse.getToken());
        return user;
    }

    /**
     * Handle exit request
     */
    private boolean handleExit() {
        outputFormatter.printHeader("Exit API Authentication Test");

        boolean confirmExit = inputHandler.getBoolean("Are you sure you want to exit the API authentication test?");

        if (confirmExit) {
            outputFormatter.printInfo("Thank you for testing FIN API authentication. Goodbye!");
            return true;
        } else {
            outputFormatter.printInfo("Returning to API authentication test menu");
            return false;
        }
    }

    /**
     * Display welcome message after successful authentication
     */
    private void displayWelcomeMessage() {
        outputFormatter.printSeparator();
        outputFormatter.printSuccess("API Authentication Test successful!");
        outputFormatter.printInfo("You are now logged in as: " + applicationState.getCurrentUserDisplayName());
        outputFormatter.printInfo("JWT Token stored for API calls");
        outputFormatter.printSeparator();

        inputHandler.waitForEnter("Press Enter to continue to the main application");
    }

    /**
     * Logout current user (API call)
     */
    public void logout() {
        User currentUser = applicationState.getCurrentUser();
        if (currentUser != null && currentUser.getToken() != null) {
            try {
                // Make logout API call
                ApiResponse<?> response = makeApiCall("/api/v1/auth/logout", "POST", null, null);

                if (response.isSuccess()) {
                    outputFormatter.printSuccess("API Logout successful");
                } else {
                    outputFormatter.printWarning("API Logout failed: " + response.getMessage());
                }
            } catch (Exception e) {
                outputFormatter.printError("API Logout error: " + e.getMessage());
            }
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