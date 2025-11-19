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

package fin.api.config;

import static spark.Spark.exception;
import static spark.Spark.notFound;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fin.security.SecurityUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception Configuration class
 * Handles global exception handling and 404 responses for the API
 */
public class ExceptionConfig {

    /**
     * Configures global exception handlers
     */
    public static void configure() {
        Gson gson = new GsonBuilder().create();

        // Global exception handler with security considerations
        exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            response.type("application/json");

            // Sanitize error message to prevent information leakage
            String safeMessage = SecurityUtils.sanitizeErrorMessage(exception);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", safeMessage);
            error.put("timestamp", System.currentTimeMillis());
            error.put("path", request.pathInfo());

            response.body(gson.toJson(error));

            // Log security events
            if (exception instanceof SecurityException) {
                SecurityUtils.logSecurityEvent("SECURITY_EXCEPTION",
                    safeMessage,
                    request.ip(),
                    request.userAgent());
            } else {
                // Log the error for debugging (without sensitive data)
                System.err.println("❌ API Error: " + safeMessage);
            }
        });

        // Not found handler
        notFound((request, response) -> {
            response.type("application/json");

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "The requested endpoint does not exist");
            error.put("path", request.pathInfo());
            error.put("timestamp", System.currentTimeMillis());

            return gson.toJson(error);
        });
    }
}