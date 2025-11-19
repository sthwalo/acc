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
import fin.api.controllers.CompanyController;
import fin.api.dto.requests.CompanyRequest;
import fin.api.dto.responses.ApiResponse;
import fin.api.middleware.AuthMiddleware;
import fin.api.util.RequestValidator;
import spark.Spark;

/**
 * Company Routes
 * Defines all company-related API endpoints
 */
public class CompanyRoutes {

    /**
     * Register company routes
     * @param gson the JSON serializer
     * @param companyController the company controller
     * @param authMiddleware the auth middleware
     */
    public static void register(Gson gson, CompanyController companyController, AuthMiddleware authMiddleware) {

        // GET /api/v1/companies - Get all companies (admin only)
        Spark.get("/api/v1/companies", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Get all companies
                ApiResponse<?> apiResponse = companyController.getAllCompanies();

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(500);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // GET /api/v1/companies/user - Get companies for authenticated user
        Spark.get("/api/v1/companies/user", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication and get user ID
                Long userId = authMiddleware.getAuthenticatedUserId(request);
                if (userId == null) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Get user companies
                ApiResponse<?> apiResponse = companyController.getCompaniesForUser(userId);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(500);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // GET /api/v1/companies/:id - Get company by ID
        Spark.get("/api/v1/companies/:id", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Parse company ID
                Long companyId;
                try {
                    companyId = Long.parseLong(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid company ID format");
                    return gson.toJson(errorResponse);
                }

                // Get company
                ApiResponse<?> apiResponse = companyController.getCompanyById(companyId);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(404);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // POST /api/v1/companies - Create new company
        Spark.post("/api/v1/companies", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Parse request body
                CompanyRequest companyRequest = gson.fromJson(request.body(), CompanyRequest.class);

                // Validate request
                if (!RequestValidator.isValidCompanyRequest(companyRequest)) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid company request");
                    return gson.toJson(errorResponse);
                }

                // Create company
                ApiResponse<?> apiResponse = companyController.createCompany(companyRequest);

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

        // PUT /api/v1/companies/:id - Update company
        Spark.put("/api/v1/companies/:id", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Parse company ID
                Long companyId;
                try {
                    companyId = Long.parseLong(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid company ID format");
                    return gson.toJson(errorResponse);
                }

                // Parse request body
                CompanyRequest companyRequest = gson.fromJson(request.body(), CompanyRequest.class);

                // Validate request
                if (!RequestValidator.isValidCompanyRequest(companyRequest)) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid company request");
                    return gson.toJson(errorResponse);
                }

                // Update company
                ApiResponse<?> apiResponse = companyController.updateCompany(companyId, companyRequest);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(404);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Internal server error");
                return gson.toJson(errorResponse);
            }
        });

        // DELETE /api/v1/companies/:id - Delete company
        Spark.delete("/api/v1/companies/:id", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Authentication required");
                    return gson.toJson(errorResponse);
                }

                // Parse company ID
                Long companyId;
                try {
                    companyId = Long.parseLong(request.params(":id"));
                } catch (NumberFormatException e) {
                    response.status(400);
                    ApiResponse<Void> errorResponse = new ApiResponse<>(false, null, "Invalid company ID format");
                    return gson.toJson(errorResponse);
                }

                // Delete company
                ApiResponse<?> apiResponse = companyController.deleteCompany(companyId);

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(404);
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