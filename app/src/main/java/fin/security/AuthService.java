/*
 * FIN Financial Management System - Security Framework
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
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

    // Session timeout: 8 hours
    private static final long SESSION_TIMEOUT_MINUTES = 480;

    public AuthService() {
        this.userRepository = new UserRepository(DatabaseConfig.getDatabaseUrl());
        this.activeSessions = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();

        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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
    public boolean hasRole(Session session, String requiredRole, Long companyId) {
        if (session == null) {
            return false;
        }

        User user = session.getUser();

        // Check if user belongs to the company
        if (!user.getCompanyId().equals(companyId)) {
            return false;
        }

        // Check role
        return user.getRole().equalsIgnoreCase(requiredRole) ||
               user.getRole().equalsIgnoreCase("ADMIN");
    }

    /**
     * Check if user can access company data
     */
    public boolean canAccessCompany(Session session, Long companyId) {
        if (session == null || companyId == null) {
            return false;
        }

        return session.getUser().getCompanyId().equals(companyId) ||
               session.getUser().getRole().equalsIgnoreCase("ADMIN");
    }

    /**
     * Create new user (admin only)
     */
    public User createUser(String email, String password, String firstName, String lastName,
                          String role, Long companyId, String createdBy) {

        // Validate input
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.length() < 8) {
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
        user.setCompanyId(companyId);
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
        if (newPassword == null || newPassword.length() < 8) {
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
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String hashPassword(String password, String salt) {
        String combined = password + salt;
        byte[] hash = sha256.digest(combined.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Authentication result
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String sessionToken;

        public AuthResult(boolean success, String message, String sessionToken) {
            this.success = success;
            this.message = message;
            this.sessionToken = sessionToken;
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

        public Session(User user, String token, LocalDateTime createdAt) {
            this.user = user;
            this.token = token;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
        }

        public User getUser() { return user; }
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