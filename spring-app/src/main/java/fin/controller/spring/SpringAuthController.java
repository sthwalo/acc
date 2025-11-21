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

package fin.controller.spring;

import fin.model.User;
import fin.service.spring.JwtService;
import fin.service.spring.SpringUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for Authentication operations
 * Handles login and registration endpoints
 */
@RestController
//@RequestMapping("/api/v1/auth")
public class SpringAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SpringAuthController.class);

    private final SpringUserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SpringAuthController(SpringUserService userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        logger.info("SpringAuthController initialized successfully");
    }

    /**
     * Verify password against stored hash - handles both BCrypt and legacy SHA-256 formats
     */
    private boolean verifyPassword(String plainPassword, String storedHash, String salt) {
        // Check if it's a BCrypt hash (starts with $2a$, $2b$, or $2y$)
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return passwordEncoder.matches(plainPassword, storedHash);
        }

        // Legacy SHA-256 + salt + Base64 format
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String combined = plainPassword + salt;
            byte[] hash = sha256.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computedHash = Base64.getEncoder().encodeToString(hash);
            return computedHash.equals(storedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Login endpoint
     */
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Find user by email
            User user = userService.getUserByEmail(loginRequest.getEmail());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid email or password"
                ));
            }

            // Verify password - handle both BCrypt and legacy SHA-256 formats
            boolean passwordValid = verifyPassword(loginRequest.getPassword(), user.getPasswordHash(), user.getSalt());
            if (!passwordValid) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid email or password"
                ));
            }

            // Check if user is active
            if (!user.isActive()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Account is deactivated"
                ));
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userService.updateUser(user);

            // Generate JWT token
            String token = jwtService.generateToken(user);

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", user);
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Login failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Register endpoint
     */
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Check if user already exists
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email already exists"
                ));
            }

            // Create new user
            User newUser = new User();
            newUser.setEmail(registerRequest.getEmail());
            newUser.setFirstName(registerRequest.getFirstName());
            newUser.setLastName(registerRequest.getLastName());
            newUser.setRole("USER");
            newUser.setPlanId(registerRequest.getPlanId());
            newUser.setActive(true);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            newUser.setCreatedBy("FIN");
            newUser.setUpdatedBy("FIN");

            // Hash password
            String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
            newUser.setPasswordHash(hashedPassword);

            // Generate salt (for backwards compatibility)
            newUser.setSalt("default_salt");

            // Save user
            User savedUser = userService.createUser(newUser);

            // Generate JWT token
            String token = jwtService.generateToken(savedUser);

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", savedUser);
            response.put("message", "Registration successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Registration failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Get current user endpoint
     */
    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestAttribute("user") User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint (client-side token removal)
     */
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Logged out successfully"
        ));
    }

    // DTO classes
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private Long planId;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }
}