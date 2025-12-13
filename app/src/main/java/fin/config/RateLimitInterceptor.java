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

package fin.config;

import fin.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/**
 * Simple Rate Limiting HTTP Interceptor
 *
 * Applies rate limits to API endpoints and returns proper HTTP 429 responses with headers.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitService rateLimitService;

    @Autowired
    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Determine which bucket to use based on endpoint
        boolean allowed = false;
        long remainingTokens = 0;
        int limit = 0;

        if (isLoginEndpoint(requestURI, method)) {
            allowed = rateLimitService.checkLoginLimit();
            remainingTokens = rateLimitService.getLoginRemainingTokens();
            limit = 5; // 5 requests per minute
        } else if (isPaymentEndpoint(requestURI, method)) {
            allowed = rateLimitService.checkPaymentLimit();
            remainingTokens = rateLimitService.getPaymentRemainingTokens();
            limit = 10; // 10 requests per minute
        } else if (isApiEndpoint(requestURI)) {
            allowed = rateLimitService.checkApiLimit();
            remainingTokens = rateLimitService.getApiRemainingTokens();
            limit = 100; // 100 requests per minute
        } else {
            // No rate limiting for non-API endpoints
            return true;
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remainingTokens)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));

        if (!allowed) {
            logger.warn("Rate limit exceeded for {} {}", method, requestURI);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in 60 seconds.\"}");
            return false;
        }

        return true;
    }

    /**
     * Check if this is a login endpoint
     */
    private boolean isLoginEndpoint(String uri, String method) {
        return uri.contains("/login") || uri.contains("/auth") && "POST".equals(method);
    }

    /**
     * Check if this is a payment endpoint
     */
    private boolean isPaymentEndpoint(String uri, String method) {
        return uri.contains("/payment") || uri.contains("/transaction") && "POST".equals(method);
    }

    /**
     * Check if this is a general API endpoint
     */
    private boolean isApiEndpoint(String uri) {
        return uri.startsWith("/api/");
    }
}
