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
import fin.api.controllers.TransactionController;
import fin.api.middleware.AuthMiddleware;
import spark.Spark;

/**
 * Transaction Routes
 * Defines all transaction-related API endpoints
 */
public class TransactionRoutes {

    /**
     * Register transaction routes
     * @param gson the JSON serializer
     * @param transactionController the transaction controller
     * @param authMiddleware the auth middleware
     */
    public static void register(Gson gson, TransactionController transactionController, AuthMiddleware authMiddleware) {
        // GET /api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions - Get all transactions
        Spark.get("/api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Authentication required"));
                }

                // Parse parameters
                Long companyId = Long.parseLong(request.params(":companyId"));
                Long fiscalPeriodId = Long.parseLong(request.params(":fiscalPeriodId"));
                int page = Integer.parseInt(request.queryParams("page") != null ? request.queryParams("page") : "0");
                int size = Integer.parseInt(request.queryParams("size") != null ? request.queryParams("size") : "50");

                // Get transactions
                fin.api.dto.responses.ApiResponse<?> apiResponse = transactionController.getTransactions(companyId, fiscalPeriodId, page, size);

                // Set response status
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(500);
                }

                return gson.toJson(apiResponse);

            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Invalid company ID or fiscal period ID format"));
            } catch (Exception e) {
                response.status(500);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Internal server error"));
            }
        });

        // GET /api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions/:id - Get transaction by ID
        Spark.get("/api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions/:id", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Authentication required"));
                }

                // Parse parameters
                Long companyId = Long.parseLong(request.params(":companyId"));
                Long fiscalPeriodId = Long.parseLong(request.params(":fiscalPeriodId"));
                Long id = Long.parseLong(request.params(":id"));

                // Get transaction
                fin.api.dto.responses.ApiResponse<?> apiResponse = transactionController.getTransaction(companyId, fiscalPeriodId, id);

                // Set response status
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(404);
                }

                return gson.toJson(apiResponse);

            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Invalid ID format"));
            } catch (Exception e) {
                response.status(500);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Internal server error"));
            }
        });

        // GET /api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions/search - Search transactions
        Spark.get("/api/v1/companies/:companyId/fiscal-periods/:fiscalPeriodId/transactions/search", (request, response) -> {
            response.type("application/json");

            try {
                // Check authentication
                if (!authMiddleware.isAuthenticated(request)) {
                    response.status(401);
                    return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Authentication required"));
                }

                // Parse parameters
                Long companyId = Long.parseLong(request.params(":companyId"));
                Long fiscalPeriodId = Long.parseLong(request.params(":fiscalPeriodId"));
                String query = request.queryParams("query");

                if (query == null || query.trim().isEmpty()) {
                    response.status(400);
                    return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Query parameter is required"));
                }

                // Search transactions
                fin.api.dto.responses.ApiResponse<?> apiResponse = transactionController.searchTransactions(companyId, fiscalPeriodId, query);

                // Set response status
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(500);
                }

                return gson.toJson(apiResponse);

            } catch (NumberFormatException e) {
                response.status(400);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Invalid company ID or fiscal period ID format"));
            } catch (Exception e) {
                response.status(500);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Internal server error"));
            }
        });
    }
}