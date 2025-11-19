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

import static spark.Spark.before;
import static spark.Spark.options;
import fin.security.SecurityUtils;

/**
 * CORS Configuration class
 * Handles Cross-Origin Resource Sharing setup for the API
 */
public class CorsConfig {

    /**
     * Configures CORS headers and preflight handling
     * Sets up security headers and origin validation
     */
    public static void configure() {
        // Configure CORS before filter
        before((request, response) -> {
            String origin = request.headers("Origin");

            // Validate origin against whitelist
            if (SecurityUtils.isValidOrigin(origin)) {
                response.header("Access-Control-Allow-Origin", origin);
            }

            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Max-Age", "86400"); // 24 hours

            SecurityUtils.addSecurityHeaders(response);

            // Log security-sensitive requests
            if (SecurityUtils.containsSensitiveHeaders(request)) {
                SecurityUtils.logSecurityEvent("SENSITIVE_HEADERS",
                    "Request contains sensitive headers",
                    request.ip(),
                    request.userAgent());
            }
        });

        // Handle preflight OPTIONS requests
        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });
    }
}