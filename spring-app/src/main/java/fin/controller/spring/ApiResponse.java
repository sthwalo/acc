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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized API response wrapper for all backend endpoints.
 * Ensures consistent response format across the entire application.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Integer count;
    private final String errorCode;
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;

    // Private constructor - use factory methods
    private ApiResponse(boolean success, String message, T data, Integer count,
                       String errorCode, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.count = count;
        this.errorCode = errorCode;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
    }

    // Factory methods for different response types

    /**
     * Success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, null, null);
    }

    /**
     * Success response with data and count
     */
    public static <T> ApiResponse<T> success(T data, int count) {
        return new ApiResponse<>(true, null, data, count, null, null);
    }

    /**
     * Success response with message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, null);
    }

    /**
     * Success response with message, data, and count
     */
    public static <T> ApiResponse<T> success(String message, T data, int count) {
        return new ApiResponse<>(true, message, data, count, null, null);
    }

    /**
     * Success response with message only
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null, null, null);
    }

    /**
     * Error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, null, null);
    }

    /**
     * Error response with message and error code
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, null, errorCode, null);
    }

    /**
     * Error response with message, error code, and metadata
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, Map<String, Object> metadata) {
        return new ApiResponse<>(false, message, null, null, errorCode, metadata);
    }

    /**
     * Empty data response (for cases like no transactions found)
     */
    public static <T> ApiResponse<T> empty(String message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("empty", true);
        return new ApiResponse<>(true, message, null, 0, null, metadata);
    }

    /**
     * Empty data response with custom metadata
     */
    public static <T> ApiResponse<T> empty(String message, Map<String, Object> metadata) {
        Map<String, Object> combinedMetadata = new HashMap<>();
        if (metadata != null) {
            combinedMetadata.putAll(metadata);
        }
        combinedMetadata.put("empty", true);
        return new ApiResponse<>(true, message, null, 0, null, combinedMetadata);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Integer getCount() { return count; }
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
}