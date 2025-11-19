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

package fin.security;

import fin.service.JwtService;
import fin.service.UserService;
import fin.model.User;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security utilities implementing Spring Security best practices
 * Adapted for Spark Java framework
 */
public class SecurityUtils {

    // Rate limiting
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    private static final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    // Security headers
    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:5173",
        "https://fin-app.com"
    };

    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
        "authorization", "x-api-key", "cookie", "set-cookie"
    ));

    /**
     * Validate JWT token from request
     */
    public static User validateJwtToken(Request request, JwtService jwtService, UserService userService) {
        String authHeader = request.headers("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // Validate token structure and expiry
            User user = jwtService.validateToken(token);

            // Verify user still exists and is active
            User currentUser = userService.getCurrentUser(user.getId());
            if (currentUser == null || !currentUser.isActive()) {
                throw new SecurityException("User account is inactive or deleted");
            }

            return currentUser;
        } catch (Exception e) {
            throw new SecurityException("Invalid or expired token: " + e.getMessage());
        }
    }

    /**
     * Check rate limiting for login attempts
     */
    public static void checkRateLimit(String identifier) {
        LoginAttempt attempt = loginAttempts.get(identifier);

        if (attempt != null) {
            // Check if account is locked
            if (attempt.isLocked() && !attempt.isLockExpired()) {
                long remainingTime = (attempt.getLockExpiry() - System.currentTimeMillis()) / 1000 / 60;
                throw new SecurityException("Account temporarily locked due to too many failed attempts. Try again in " + remainingTime + " minutes.");
            }

            // Reset if lock has expired
            if (attempt.isLocked() && attempt.isLockExpired()) {
                loginAttempts.remove(identifier);
                return;
            }
        }
    }

    /**
     * Record failed login attempt
     */
    public static void recordFailedLogin(String identifier) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(identifier,
            k -> new LoginAttempt());

        attempt.incrementAttempts();

        if (attempt.getAttempts() >= MAX_LOGIN_ATTEMPTS) {
            attempt.lock();
        }
    }

    /**
     * Clear login attempts on successful login
     */
    public static void clearLoginAttempts(String identifier) {
        loginAttempts.remove(identifier);
    }

    /**
     * Validate CORS origin
     */
    public static boolean isValidOrigin(String origin) {
        if (origin == null) return false;

        for (String allowedOrigin : ALLOWED_ORIGINS) {
            if (allowedOrigin.equals(origin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add security headers to response
     */
    public static void addSecurityHeaders(Response response) {
        // Prevent clickjacking
        response.header("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.header("X-Content-Type-Options", "nosniff");

        // Enable XSS protection
        response.header("X-XSS-Protection", "1; mode=block");

        // Strict transport security (for HTTPS)
        response.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content Security Policy
        response.header("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' http://localhost:* https://api.fin-app.com; " +
            "frame-ancestors 'none';");

        // Referrer policy
        response.header("Referrer-Policy", "strict-origin-when-cross-origin");

        // Feature policy
        response.header("Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=()");

        // Remove server information
        response.header("Server", "");
    }

    /**
     * Sanitize error messages to prevent information leakage
     */
    public static String sanitizeErrorMessage(Exception e) {
        String message = e.getMessage();

        // Remove sensitive information
        if (message != null) {
            message = message.replaceAll("(?i)(password|token|key|secret|credential)", "[REDACTED]");
            message = message.replaceAll("SQL\\s+.*", "Database error");
            message = message.replaceAll("java\\..*", "System error");
        }

        return message != null ? message : "An error occurred";
    }

    /**
     * Validate input data for common security issues
     */
    public static void validateInput(String input, String fieldName) {
        if (input == null) return;

        // Check for SQL injection patterns
        if (input.matches(".*(\\b(union|select|insert|update|delete|drop|create|alter)\\b).*")) {
            throw new SecurityException("Invalid input detected in " + fieldName);
        }

        // Check for script injection
        if (input.matches(".*(<script|javascript:|on\\w+=|\\b(alert|eval|function)\\b).*")) {
            throw new SecurityException("Invalid input detected in " + fieldName);
        }

        // Check for extremely long input (potential DoS)
        if (input.length() > 10000) {
            throw new SecurityException("Input too long for " + fieldName);
        }
    }

    /**
     * Log security events
     */
    public static void logSecurityEvent(String event, String details, String ipAddress, String userAgent) {
        System.out.println(String.format("[SECURITY] %s - %s - IP: %s - UA: %s",
            event, details, ipAddress != null ? ipAddress : "unknown",
            userAgent != null ? userAgent : "unknown"));
    }

    /**
     * Check if request contains sensitive headers that should be filtered
     */
    public static boolean containsSensitiveHeaders(Request request) {
        for (String header : SENSITIVE_HEADERS) {
            if (request.headers(header) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inner class for tracking login attempts
     */
    private static class LoginAttempt {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private volatile boolean locked = false;
        private volatile long lockExpiry = 0;

        public void incrementAttempts() {
            attempts.incrementAndGet();
        }

        public int getAttempts() {
            return attempts.get();
        }

        public void lock() {
            locked = true;
            lockExpiry = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
        }

        public boolean isLocked() {
            return locked;
        }

        public boolean isLockExpired() {
            return System.currentTimeMillis() > lockExpiry;
        }

        public long getLockExpiry() {
            return lockExpiry;
        }
    }
}