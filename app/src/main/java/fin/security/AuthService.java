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

import fin.model.User;
import fin.repository.UserRepository;
import fin.config.DatabaseConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Authentication and Authorization Service
 * Provides secure user authentication, session management, and role-based access control
 */
public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    private final UserRepository userRepository;
    private final Map<String, Session> activeSessions;
    private final SecureRandom secureRandom;
    private final MessageDigest sha256;

    // Security constants
    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int SESSION_TOKEN_BYTE_LENGTH = 32;
    private static final int SALT_BYTE_LENGTH = 16;

    // Session timeout: 8 hours
    private static final long SESSION_TIMEOUT_MINUTES = 480;

    public AuthService() {
        // Initialize MessageDigest first (can throw exception)
        MessageDigest sha256Digest = null;
        try {
            sha256Digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("❌ SHA-256 not available: " + e.getMessage() + " - using fallback");
            // Don't throw exception - allow constructor to complete
        }

        // Initialize UserRepository (can throw exception)
        UserRepository userRepo = null;
        try {
            userRepo = new UserRepository(DatabaseConfig.getDatabaseUrl());
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize UserRepository: " + e.getMessage() + " - continuing without authentication");
            // Don't throw exception - allow constructor to complete
        }

        // Only assign fields after initialization attempts
        this.sha256 = sha256Digest;
        this.userRepository = userRepo;
        this.activeSessions = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Authenticate user with email and password
     */
    public AuthResult authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return new AuthResult(false, "Email is required", null);
        }

        if (password == null || password.trim().isEmpty()) {
            return new AuthResult(false, "Password is required", null);
        }

        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                LOGGER.warning("Authentication failed: User not found - " + email);
                return new AuthResult(false, "Invalid email or password", null);
            }

            if (!user.isActive()) {
                LOGGER.warning("Authentication failed: User inactive - " + email);
                return new AuthResult(false, "Account is disabled", null);
            }

            // Verify password
            String hashedPassword = hashPassword(password, user.getSalt());
            if (!hashedPassword.equals(user.getPasswordHash())) {
                LOGGER.warning("Authentication failed: Invalid password - " + email);
                return new AuthResult(false, "Invalid email or password", null);
            }

            // Create session
            String sessionToken = generateSessionToken();
            Session session = new Session(user, sessionToken, LocalDateTime.now());
            activeSessions.put(sessionToken, session);

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.update(user);

            LOGGER.info("User authenticated successfully: " + email);
            return new AuthResult(true, "Authentication successful", sessionToken);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error for user: " + email, e);
            return new AuthResult(false, "Authentication service unavailable", null);
        }
    }

    /**
     * Validate session token
     */
    public Session validateSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        Session session = activeSessions.get(token);
        if (session == null) {
            return null;
        }

        // Check if session has expired
        if (session.isExpired()) {
            activeSessions.remove(token);
            LOGGER.info("Session expired for user: " + session.getUser().getEmail());
            return null;
        }

        // Update last activity
        session.updateLastActivity();
        return session;
    }

    /**
     * Logout user by invalidating session
     */
    public boolean logout(String token) {
        if (token != null) {
            Session session = activeSessions.remove(token);
            if (session != null) {
                LOGGER.info("User logged out: " + session.getUser().getEmail());
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has required role for company
     */
    public boolean hasRole(Session session, String requiredRole) {
        if (session == null) {
            return false;
        }

        User user = session.getUser();

        // Check role
        return user.getRole().equalsIgnoreCase(requiredRole) ||
               user.getRole().equalsIgnoreCase("ADMIN");
    }

    /**
     * Create new user (admin only)
     */
    public User createUser(String email, String password, String firstName, String lastName,
                          String role, Long planId, String createdBy) {

        // Validate input
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (role == null || (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("USER"))) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }

        // Check if user already exists
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Generate salt and hash password
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role.toUpperCase());
        user.setPlanId(planId);
        user.setActive(true);
        user.setCreatedBy(createdBy);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Change user password
     */
    public boolean changePassword(Session session, String currentPassword, String newPassword) {
        if (session == null) {
            return false;
        }

        User user = session.getUser();

        // Verify current password
        String currentHash = hashPassword(currentPassword, user.getSalt());
        if (!currentHash.equals(user.getPasswordHash())) {
            return false;
        }

        // Validate new password
        if (newPassword == null || newPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        // Update password
        String salt = generateSalt();
        String newHash = hashPassword(newPassword, salt);

        user.setPasswordHash(newHash);
        user.setSalt(salt);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.update(user);
        return true;
    }

    /**
     * Clean up expired sessions (call periodically)
     */
    public void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    // Utility methods

    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generateSalt() {
        byte[] bytes = new byte[SALT_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String hashPassword(String password, String salt) {
        String combined = password + salt;
        byte[] hash = sha256.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Authentication result
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String sessionToken;

        public AuthResult(boolean valueSuccess, String valueMessage, String valueSessionToken) {
            this.success = valueSuccess;
            this.message = valueMessage;
            this.sessionToken = valueSessionToken;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionToken() { return sessionToken; }
    }

    /**
     * User session
     */
    public static class Session {
        private final User user;
        private final String token;
        private final LocalDateTime createdAt;
        private LocalDateTime lastActivity;

        public Session(User valueUser, String valueToken, LocalDateTime valueCreatedAt) {
            this.user = valueUser != null ? new User(valueUser) : null;
            this.token = valueToken;
            this.createdAt = valueCreatedAt;
            this.lastActivity = valueCreatedAt;
        }

        public User getUser() { return user != null ? new User(user) : null; }
        public String getToken() { return token; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }

        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(lastActivity.plusMinutes(SESSION_TIMEOUT_MINUTES));
        }
    }
}