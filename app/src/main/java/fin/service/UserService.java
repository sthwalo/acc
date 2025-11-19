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

import fin.model.User;
import fin.model.UserCompany;
import fin.model.Plan;
import fin.repository.UserRepository;
import fin.repository.UserCompanyRepository;
import fin.repository.PlanRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * User authentication and management service
 * Handles user registration, login, and session management with company access and plan validation
 */
public class UserService {
    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final PlanRepository planRepository;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;

    public UserService(String dbUrl, UserCompanyRepository userCompanyRepository, PlanRepository planRepository) {
        this.userRepository = new UserRepository(dbUrl);
        this.userCompanyRepository = userCompanyRepository;
        this.planRepository = planRepository;
    }

    /**
     * Register a new user
     */
    public User registerUser(String email, String password, String firstName, String lastName, Long planId) {
        // Validate input
        validateRegistrationData(email, password, firstName, lastName);

        // Validate plan exists and is active
        Plan plan = planRepository.findById(planId).orElseThrow(() ->
            new IllegalArgumentException("Invalid plan ID: " + planId));

        if (!plan.isActive()) {
            throw new IllegalArgumentException("Plan is not active: " + plan.getName());
        }

        // Check if user already exists
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(email.toLowerCase().trim());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setRole("USER"); // Default role
        user.setPlanId(planId);
        user.setActive(true);
        user.setCreatedBy("FIN");
        user.setUpdatedBy("FIN");

        // Hash password
        String salt = generateSalt();
        String passwordHash = hashPassword(password, salt);
        user.setSalt(salt);
        user.setPasswordHash(passwordHash);

        // Save user
        User savedUser = userRepository.save(user);

        // Update last login time
        savedUser.setLastLoginAt(LocalDateTime.now());
        userRepository.update(savedUser);

        return savedUser;
    }

    /**
     * Authenticate user login
     */
    public User authenticateUser(String email, String password) {
        // Find user by email
        User user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        // Verify password
        String hashedPassword = hashPassword(password, user.getSalt());
        if (!hashedPassword.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedBy("FIN");
        userRepository.update(user);

        return user;
    }

    /**
     * Get current user by ID (for session validation)
     */
    public User getCurrentUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        return user;
    }

    /**
     * Get user by ID (alias for getCurrentUser for Spring Boot compatibility)
     */
    public User getUserById(Long userId) {
        return getCurrentUser(userId);
    }

    /**
     * Update user profile
     */
    public User updateUserProfile(Long userId, String firstName, String lastName, String email) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(email.toLowerCase().trim())) {
            User existingUser = userRepository.findByEmail(email.toLowerCase().trim());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new IllegalArgumentException("Email address is already in use");
            }
            user.setEmail(email.toLowerCase().trim());
        }

        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setUpdatedBy("USER");
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Change user password
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Verify current password
        String currentHashedPassword = hashPassword(currentPassword, user.getSalt());
        if (!currentHashedPassword.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        validatePassword(newPassword);

        // Hash new password
        String salt = generateSalt();
        String newPasswordHash = hashPassword(newPassword, salt);

        user.setPasswordHash(newPasswordHash);
        user.setSalt(salt);
        user.setUpdatedBy("USER");
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Deactivate user account
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setActive(false);
        user.setUpdatedBy("FIN");
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Generate a random salt for password hashing
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash password with salt using SHA-256
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing algorithm not available", e);
        }
    }

    /**
     * Validate registration data
     */
    private void validateRegistrationData(String email, String password, String firstName, String lastName) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }

    /**
     * Check if user can access dashboard (validates plan and active status)
     */
    public boolean canUserAccessDashboard(Long userId) {
        User user = getCurrentUser(userId);
        return planRepository.canPlanAccessFeature(user.getPlanId(), "dashboard");
    }

    /**
     * Check if user has access to a specific company
     */
    public boolean canUserAccessCompany(Long userId, Long companyId) {
        return userCompanyRepository.hasUserAccessToCompany(userId, companyId);
    }

    /**
     * Check if user has a specific role in a company
     */
    public boolean hasUserRoleInCompany(Long userId, Long companyId, String role) {
        return userCompanyRepository.hasUserRoleInCompany(userId, companyId, role);
    }

    /**
     * Get all companies a user has access to
     */
    public List<UserCompany> getUserCompanies(Long userId) {
        return userCompanyRepository.findCompaniesByUser(userId);
    }

    /**
     * Grant user access to a company
     */
    public UserCompany grantCompanyAccess(Long userId, Long companyId, String role, String grantedBy) {
        // Validate user exists
        getCurrentUser(userId);

        // Check plan allows multiple companies if user already has companies
        int currentCompanyCount = userCompanyRepository.getCompanyCountForUser(userId);
        if (currentCompanyCount > 0) {
            User user = getCurrentUser(userId);
            if (!planRepository.canPlanAccessMultipleCompanies(user.getPlanId())) {
                throw new IllegalArgumentException("User's plan does not allow access to multiple companies");
            }

            int maxCompanies = planRepository.getMaxCompaniesForPlan(user.getPlanId());
            if (currentCompanyCount >= maxCompanies) {
                throw new IllegalArgumentException("User has reached maximum company limit for their plan");
            }
        }

        return userCompanyRepository.grantUserAccessToCompany(userId, companyId, role, grantedBy);
    }

    /**
     * Revoke user access from a company
     */
    public void revokeCompanyAccess(Long userId, Long companyId, String revokedBy) {
        // Validate user exists
        getCurrentUser(userId);

        userCompanyRepository.removeUserFromCompany(userId, companyId);
    }

    /**
     * Check if user can access a specific feature based on their plan
     */
    public boolean canUserAccessFeature(Long userId, String feature) {
        User user = getCurrentUser(userId);
        return planRepository.canPlanAccessFeature(user.getPlanId(), feature);
    }

    /**
     * Get user's plan details
     */
    public Plan getUserPlan(Long userId) {
        User user = getCurrentUser(userId);
        return planRepository.findById(user.getPlanId()).orElseThrow(() ->
            new IllegalArgumentException("User's plan not found"));
    }

    /**
     * Validate dashboard access for user and company
     * This is the main method called before granting dashboard access
     */
    public void validateDashboardAccess(Long userId, Long companyId) {
        // Check user can access dashboard
        if (!canUserAccessDashboard(userId)) {
            throw new SecurityException("User's plan does not allow dashboard access");
        }

        // Check user has access to the specific company
        if (!canUserAccessCompany(userId, companyId)) {
            throw new SecurityException("User does not have access to this company");
        }

        // Additional validations can be added here
        // e.g., check transaction limits, feature access, etc.
    }

    /**
     * Get user's access role for a company
     */
    public String getUserRoleInCompany(Long userId, Long companyId) {
        UserCompany userCompany = userCompanyRepository.findByUserAndCompany(userId, companyId)
            .orElseThrow(() -> new IllegalArgumentException("User does not have access to this company"));

        return userCompany.getRole();
    }

    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        // Add more password validation rules as needed
    }
}