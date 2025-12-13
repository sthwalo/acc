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

package fin.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Simple Rate Limiting Service
 *
 * Provides basic API protection with in-memory rate limiting.
 * No plans, no Redis, no complexity - just simple resource safety.
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final Bucket loginBucket;
    private final Bucket paymentBucket;
    private final Bucket apiBucket;

    @Autowired
    public RateLimitService(Bucket loginBucket, Bucket paymentBucket, Bucket apiBucket) {
        this.loginBucket = loginBucket;
        this.paymentBucket = paymentBucket;
        this.apiBucket = apiBucket;
    }

    /**
     * Check rate limit for login endpoints (5/minute)
     */
    public boolean checkLoginLimit() {
        ConsumptionProbe probe = loginBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            logger.warn("Login rate limit exceeded. Remaining tokens: {}", probe.getRemainingTokens());
            return false;
        }
        return true;
    }

    /**
     * Check rate limit for payment endpoints (10/minute)
     */
    public boolean checkPaymentLimit() {
        ConsumptionProbe probe = paymentBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            logger.warn("Payment rate limit exceeded. Remaining tokens: {}", probe.getRemainingTokens());
            return false;
        }
        return true;
    }

    /**
     * Check rate limit for general API endpoints (100/minute)
     */
    public boolean checkApiLimit() {
        ConsumptionProbe probe = apiBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            logger.warn("API rate limit exceeded. Remaining tokens: {}", probe.getRemainingTokens());
            return false;
        }
        return true;
    }

    /**
     * Get remaining tokens for login bucket
     */
    public long getLoginRemainingTokens() {
        return loginBucket.getAvailableTokens();
    }

    /**
     * Get remaining tokens for payment bucket
     */
    public long getPaymentRemainingTokens() {
        return paymentBucket.getAvailableTokens();
    }

    /**
     * Get remaining tokens for API bucket
     */
    public long getApiRemainingTokens() {
        return apiBucket.getAvailableTokens();
    }
}
