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

import fin.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Example Controller demonstrating Rate Limiting integration
 *
 * This controller shows how to use the centralized RateLimitService
 * in your application controllers.
 */
@RestController
@RequestMapping("/api/example")
public class RateLimitExampleController {

    private final RateLimitService rateLimitService;

    @Autowired
    public RateLimitExampleController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Example endpoint with automatic rate limiting via interceptor
     * The RateLimitInterceptor will automatically apply appropriate limits
     */
    @GetMapping("/auto-limited")
    public ResponseEntity<?> autoLimitedEndpoint() {
        return ResponseEntity.ok(Map.of(
            "message", "This endpoint is automatically rate limited by the interceptor",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Example endpoint with manual rate limiting control
     * Useful when you need custom logic or different limits
     */
    @GetMapping("/manual-limited")
    public ResponseEntity<?> manualLimitedEndpoint() {

        // Manual rate limit check with API bucket
        boolean allowed = rateLimitService.checkApiLimit();
        long remaining = rateLimitService.getApiRemainingTokens();

        if (!allowed) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Rate limit exceeded",
                "remainingTokens", remaining,
                "retryAfter", 60,
                "message", "Too many API requests. Try again in 60 seconds."
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Manual rate limiting applied",
            "remainingTokens", remaining,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Example payment endpoint with strict rate limiting
     */
    @GetMapping("/payment-check")
    public ResponseEntity<?> paymentEndpointCheck() {

        boolean allowed = rateLimitService.checkPaymentLimit();
        long remaining = rateLimitService.getPaymentRemainingTokens();

        if (!allowed) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Payment rate limit exceeded",
                "remainingTokens", remaining,
                "retryAfter", 60,
                "message", "Too many payment requests. Please try again later."
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Payment request allowed",
            "remainingTokens", remaining,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Rate limit status endpoint (for monitoring/debugging)
     */
    @GetMapping("/status")
    public ResponseEntity<?> rateLimitStatus() {
        return ResponseEntity.ok(Map.of(
            "rateLimitingEnabled", "Check application.properties",
            "redisHost", "Configured via environment variables",
            "apiLimits", "100/min, 1000/hour",
            "paymentLimits", "10/min, 100/hour",
            "adminLimits", "500/min",
            "reportLimits", "50/min",
            "timestamp", System.currentTimeMillis()
        ));
    }
}