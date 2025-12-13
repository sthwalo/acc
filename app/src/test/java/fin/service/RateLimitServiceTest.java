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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService
 *
 * Tests the simple in-memory rate limiting logic
 */
@ExtendWith(MockitoExtension.class)
public class RateLimitServiceTest {

    @Mock
    private Bucket loginBucket;

    @Mock
    private Bucket paymentBucket;

    @Mock
    private Bucket apiBucket;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(loginBucket, paymentBucket, apiBucket);
    }

    @Test
    void testCheckLoginLimit_Allowed() {
        // Arrange
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(4L);
        when(loginBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(loginBucket.getAvailableTokens()).thenReturn(4L);

        // Act
        boolean result = rateLimitService.checkLoginLimit();
        long remaining = rateLimitService.getLoginRemainingTokens();

        // Assert
        assertTrue(result);
        assertEquals(4L, remaining);
    }

    @Test
    void testCheckLoginLimit_Blocked() {
        // Arrange
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getRemainingTokens()).thenReturn(0L);
        when(loginBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        boolean result = rateLimitService.checkLoginLimit();

        // Assert
        assertFalse(result);
    }

    @Test
    void testCheckPaymentLimit_Allowed() {
        // Arrange
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);
        when(paymentBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(paymentBucket.getAvailableTokens()).thenReturn(9L);

        // Act
        boolean result = rateLimitService.checkPaymentLimit();
        long remaining = rateLimitService.getPaymentRemainingTokens();

        // Assert
        assertTrue(result);
        assertEquals(9L, remaining);
    }

    @Test
    void testCheckApiLimit_Allowed() {
        // Arrange
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(99L);
        when(apiBucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(apiBucket.getAvailableTokens()).thenReturn(99L);

        // Act
        boolean result = rateLimitService.checkApiLimit();
        long remaining = rateLimitService.getApiRemainingTokens();

        // Assert
        assertTrue(result);
        assertEquals(99L, remaining);
    }
}
