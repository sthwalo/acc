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

import fin.entity.User;
import fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring Service for User operations
 * Replaces manual JDBC UserService with Spring-managed service using JPA
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get active users only
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        return userRepository.findActiveUsers();
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByEmail(username);
    }

    /**
     * Get users by role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    /**
     * Get users by plan ID
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByPlanId(Long planId) {
        return userRepository.findByPlanId(planId);
    }

    /**
     * Create a new user
     */
    public User createUser(User user) {
        // Validate uniqueness constraints
        if (user.getEmail() != null &&
            userRepository.existsByEmailAndIdNot(user.getEmail(), null)) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        if (user.getUsername() != null &&
            userRepository.existsByEmailAndIdNot(user.getUsername(), null)) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        return userRepository.save(user);
    }

    /**
     * Update an existing user
     */
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update operation");
        }

        // Check if user exists
        if (!userRepository.existsById(user.getId())) {
            throw new IllegalArgumentException("User not found with ID: " + user.getId());
        }

        // Validate uniqueness constraints (excluding current user)
        if (user.getEmail() != null &&
            userRepository.existsByEmailAndIdNot(user.getEmail(), user.getId())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        if (user.getUsername() != null &&
            userRepository.existsByEmailAndIdNot(user.getUsername(), user.getId())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        return userRepository.save(user);
    }

    /**
     * Delete a user
     */
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        return true;
    }

    /**
     * Count users by role
     */
    @Transactional(readOnly = true)
    public long countUsersByRole(String role) {
        return userRepository.countByRole(role);
    }

    /**
     * Get total count of users
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Check if user exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email) != null;
    }

    /**
     * Check if user exists by username
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByEmail(username) != null;
    }
}