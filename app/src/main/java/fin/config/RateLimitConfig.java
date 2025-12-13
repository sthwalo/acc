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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Simple in-memory rate limiting configuration
 *
 * Provides basic API protection with fixed rate limits per endpoint type.
 * No Redis, no plans, no complexity - just simple resource safety.
 */
@Configuration
public class RateLimitConfig {

    @Value("${rate.limit.login.requests-per-minute:5}")
    private int loginRequestsPerMinute;

    @Value("${rate.limit.payments.requests-per-minute:10}")
    private int paymentRequestsPerMinute;

    @Value("${rate.limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;

    /**
     * Login endpoint rate limit: 5 requests per minute
     * Protects against brute force login attempts
     */
    @Bean
    public Bucket loginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(loginRequestsPerMinute, Refill.intervally(loginRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Payment endpoint rate limit: 10 requests per minute
     * Protects financial operations from abuse
     */
    @Bean
    public Bucket paymentBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(paymentRequestsPerMinute, Refill.intervally(paymentRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * General API endpoint rate limit: 100 requests per minute
     * Protects general API endpoints from abuse
     */
    @Bean
    public Bucket apiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(apiRequestsPerMinute, Refill.intervally(apiRequestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }
}
