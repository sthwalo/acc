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

package fin.api.routes;

import com.google.gson.Gson;
import fin.api.controllers.AuthController;
import fin.api.middleware.AuthMiddleware;
import fin.api.dto.requests.LoginRequest;
import fin.api.dto.requests.RegisterRequest;
import fin.api.dto.responses.ApiResponse;
import fin.api.util.RequestValidator;
import spark.Spark;

/**
 * Authentication Routes
 * Defines all authentication-related API endpoints
 */
public class AuthRoutes {

    /**
     * Register authentication routes
     * @param gson the JSON serializer
     * @param authController the auth controller
     * @param authMiddleware the auth middleware
     */
    public static void register(Gson gson, AuthController authController, AuthMiddleware authMiddleware) {

        // GET /api/v1/health - Health check endpoint
        Spark.get("/api/v1/health", (request, response) -> {
            response.type("application/json");
            response.status(200);
            return gson.toJson(new ApiResponse<>(true, "FIN API Server is healthy", null));
        });

        // POST /api/v1/auth/login - User authentication
        Spark.post("/api/v1/auth/login", (request, response) -> {
            response.type("application/json");

            try {
                // Parse request body
                LoginRequest loginRequest = gson.fromJson(request.body(), LoginRequest.class);

                // Validate request
                if (!RequestValidator.isValidLoginRequest(loginRequest)) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid login request");
                    return gson.toJson(errorResponse);
                }

                // Process login
                ApiResponse<?> apiResponse = authController.login(loginRequest);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(401);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // POST /api/v1/auth/register - User registration
        Spark.post("/api/v1/auth/register", (request, response) -> {
            response.type("application/json");

            try {
                // Parse request body
                RegisterRequest registerRequest = gson.fromJson(request.body(), RegisterRequest.class);

                // Validate request
                if (!RequestValidator.isValidRegisterRequest(registerRequest)) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid registration request");
                    return gson.toJson(errorResponse);
                }

                // Process registration
                ApiResponse<?> apiResponse = authController.register(registerRequest);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(201);
                } else {
                    response.status(400);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // POST /api/v1/auth/logout - User logout
        Spark.post("/api/v1/auth/logout", (request, response) -> {
            response.type("application/json");

            try {
                // Get token from Authorization header
                String authHeader = request.headers("Authorization");
                String token = null;

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }

                // Process logout
                ApiResponse<?> apiResponse = authController.logout(token);

                // Set response status
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(400);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // GET /api/v1/auth/me - Get current user info
        Spark.get("/api/v1/auth/me", (request, response) -> {
            response.type("application/json");

            try {
                // Get token from Authorization header
                String authHeader = request.headers("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authorization header required");
                    return gson.toJson(errorResponse);
                }

                String token = authHeader.substring(7);

                // Get current user
                ApiResponse<?> apiResponse = authController.getCurrentUser(token);

                // Set response status
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(401);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });
    }
}