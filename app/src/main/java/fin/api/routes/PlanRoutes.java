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
import fin.api.controllers.PlanController;
import spark.Spark;

/**
 * Plan Routes
 * Defines all plan-related API endpoints
 */
public class PlanRoutes {

    /**
     * Register plan routes
     * @param gson the JSON serializer
     * @param planController the plan controller
     */
    public static void register(Gson gson, PlanController planController) {

        // GET /api/v1/plans - Get all active plans for registration
        Spark.get("/api/v1/plans", (request, response) -> {
            response.type("application/json");

            try {
                var apiResponse = planController.getAllActivePlans();

                // Set response status based on success
                if (apiResponse.isSuccess()) {
                    response.status(200);
                } else {
                    response.status(500);
                }

                return gson.toJson(apiResponse);

            } catch (Exception e) {
                response.status(500);
                return gson.toJson(new fin.api.dto.responses.ApiResponse<>(false, null, "Internal server error: " + e.getMessage()));
            }
        });
    }
}