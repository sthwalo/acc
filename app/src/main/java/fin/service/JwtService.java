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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Simple JWT token service for session management
 * Note: In production, use a proper JWT library like jjwt
 */
@Service
public class JwtService {
    @Value("${fin.jwt.secret:fin-secret-key-change-in-production}")
    private String secretKey;

    private static final long TOKEN_EXPIRY_HOURS = 24; // 24 hours
    private final Gson gson;

    public JwtService() {
        this.gson = new Gson();
    }

    /**
     * Generate JWT token for user
     */
    public String generateToken(User user) {
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        JsonObject header = new JsonObject();
        header.addProperty("alg", "HS256");
        header.addProperty("typ", "JWT");

        JsonObject payload = new JsonObject();
        payload.addProperty("user_id", user.getId());
        payload.addProperty("email", user.getEmail());
        payload.addProperty("first_name", user.getFirstName());
        payload.addProperty("last_name", user.getLastName());
        payload.addProperty("role", user.getRole());
        payload.addProperty("plan_id", user.getPlanId());
        payload.addProperty("exp", expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.addProperty("iat", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String headerEncoded = base64Encode(header.toString());
        String payloadEncoded = base64Encode(payload.toString());

        String signature = generateSignature(headerEncoded + "." + payloadEncoded);

        return headerEncoded + "." + payloadEncoded + "." + signature;
    }

    /**
     * Validate and decode JWT token
     */
    public User validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            // Verify signature
            String expectedSignature = generateSignature(header + "." + payload);
            if (!expectedSignature.equals(signature)) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            // Decode payload
            String payloadJson = base64Decode(payload);
            JsonObject payloadObj = gson.fromJson(payloadJson, JsonObject.class);

            // Check expiry
            String expStr = payloadObj.get("exp").getAsString();
            LocalDateTime expiry = LocalDateTime.parse(expStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (LocalDateTime.now().isAfter(expiry)) {
                throw new IllegalArgumentException("Token has expired");
            }

            // Create user from payload
            User user = new User();
            user.setId(payloadObj.get("user_id").getAsLong());
            user.setEmail(payloadObj.get("email").getAsString());
            user.setFirstName(payloadObj.get("first_name").getAsString());
            user.setLastName(payloadObj.get("last_name").getAsString());
            user.setRole(payloadObj.get("role").getAsString());
            if (payloadObj.has("plan_id") && !payloadObj.get("plan_id").isJsonNull()) {
                user.setPlanId(payloadObj.get("plan_id").getAsLong());
            }

            return user;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Extract user ID from token without full validation
     */
    public Long getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = base64Decode(parts[1]);
            JsonObject payloadObj = gson.fromJson(payload, JsonObject.class);

            return payloadObj.get("user_id").getAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate HMAC-SHA256 signature (proper JWT implementation)
     */
    private String generateSignature(String data) {
        try {
            // Use proper HMAC-SHA256 for JWT
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return base64Encode(new String(hash));
        } catch (java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 not available", e);
        }
    }

    /**
     * Base64 encode (URL safe)
     */
    private String base64Encode(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes());
    }

    /**
     * Base64 decode (URL safe)
     */
    private String base64Decode(String data) {
        return new String(Base64.getUrlDecoder().decode(data));
    }
}