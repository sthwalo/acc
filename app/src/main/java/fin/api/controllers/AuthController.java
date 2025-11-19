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

package fin.api.controllers;

import fin.service.UserService;
import fin.service.JwtService;
import fin.api.dto.requests.LoginRequest;
import fin.api.dto.requests.RegisterRequest;
import fin.api.dto.responses.ApiResponse;
import fin.api.dto.responses.AuthResponse;
import fin.api.util.ResponseBuilder;
import fin.model.User;

/**
 * Authentication Controller
 * Handles user authentication, registration, and token management
 */
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    /**
     * Constructor with dependency injection
     * @param userService the user service
     * @param jwtService the JWT service
     */
    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Handle user login
     * @param request login request containing email and password
     * @return API response with auth token and user info on success
     */
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        try {
            // Validate input
            if (request == null || request.getEmail() == null || request.getPassword() == null) {
                return ResponseBuilder.error("Email and password are required");
            }

            // Authenticate user
            User user = userService.authenticateUser(request.getEmail(), request.getPassword());
            if (user == null) {
                return ResponseBuilder.error("Invalid email or password");
            }

            // Generate JWT token
            String token = jwtService.generateToken(user);

            // Create user info
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
            );

            // Create auth response
            AuthResponse authResponse = new AuthResponse(token, userInfo);

            return ResponseBuilder.success(authResponse, "Login successful");

        } catch (Exception e) {
            return ResponseBuilder.error("Login failed: " + e.getMessage());
        }
    }

    /**
     * Handle user registration
     * @param request registration request containing user details
     * @return API response with auth token and user info on success
     */
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        try {
            // Validate input
            if (request == null || request.getEmail() == null || request.getPassword() == null ||
                request.getFirstName() == null || request.getLastName() == null) {
                return ResponseBuilder.error("All fields are required");
            }

            // Register user
            User user = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getPlanId()
            );

            if (user == null) {
                return ResponseBuilder.error("Registration failed");
            }

            // Generate JWT token
            String token = jwtService.generateToken(user);

            // Create user info
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
            );

            // Create auth response
            AuthResponse authResponse = new AuthResponse(token, userInfo);

            return ResponseBuilder.success(authResponse, "Registration successful");

        } catch (Exception e) {
            return ResponseBuilder.error("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Handle user logout
     * @param token the JWT token to invalidate
     * @return API response confirming logout
     */
    public ApiResponse<Void> logout(String token) {
        try {
            // For stateless JWT, logout is handled client-side by removing the token
            // In a more complex implementation, you might want to maintain a blacklist
            // For now, we just return success
            return ResponseBuilder.success(null, "Logout successful");

        } catch (Exception e) {
            return ResponseBuilder.error("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Get current authenticated user
     * @param token the JWT token from Authorization header
     * @return API response with current user info
     */
    public ApiResponse<User> getCurrentUser(String token) {
        try {
            // Validate token and get user
            User user = jwtService.validateToken(token);
            if (user == null) {
                return ResponseBuilder.error("Invalid or expired token");
            }

            // Get fresh user details from database
            User currentUser = userService.getCurrentUser(user.getId());
            return ResponseBuilder.success(currentUser, "User retrieved successfully");

        } catch (Exception e) {
            return ResponseBuilder.error("Failed to get current user: " + e.getMessage());
        }
    }
}