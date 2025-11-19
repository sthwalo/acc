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

package fin.api.middleware;

import spark.Request;
import spark.Response;
import fin.service.JwtService;

/**
 * Authentication Middleware
 * Handles authentication and authorization for API requests
 */
public class AuthMiddleware {

    private final JwtService jwtService;

    /**
     * Constructor with JWT service dependency
     */
    public AuthMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Apply authentication middleware to a request
     * @param request the HTTP request
     * @param response the HTTP response
     * @return true if authenticated, false otherwise
     */
    public boolean authenticate(Request request, Response response) {
        // TODO: Implement authentication logic
        return true; // Placeholder
    }

    /**
     * Check if request is authenticated
     * @param request the HTTP request
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated(Request request) {
        // TODO: Implement proper authentication check
        // For now, check for Authorization header
        String authHeader = request.headers("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    /**
     * Get authenticated user ID from request
     * @param request the HTTP request
     * @return user ID if authenticated, null otherwise
     */
    public Long getAuthenticatedUserId(Request request) {
        if (!isAuthenticated(request)) {
            return null;
        }

        String authHeader = request.headers("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtService.getUserIdFromToken(token);
    }
}