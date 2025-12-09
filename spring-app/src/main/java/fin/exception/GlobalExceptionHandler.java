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

package fin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import fin.dto.ApiResponse;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers.
 * Ensures consistent error responses across the entire API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle SQL exceptions
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Object>> handleSQLException(SQLException ex, WebRequest request) {
        String message = "Database operation failed";
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;

        // Provide more specific messages for common SQL errors
        if (ex.getMessage().contains("duplicate key")) {
            message = "Record already exists";
            errorCode = ErrorCode.VALIDATION_ERROR;
        } else if (ex.getMessage().contains("foreign key")) {
            message = "Referenced record does not exist";
            errorCode = ErrorCode.VALIDATION_ERROR;
        } else if (ex.getMessage().contains("connection")) {
            message = "Database connection failed";
            errorCode = ErrorCode.CONNECTION_ERROR;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sqlState", ex.getSQLState());
        metadata.put("errorCode", ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, errorCode.getCode(), metadata));
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ErrorCode.VALIDATION_ERROR.getCode()));
    }

    /**
     * Handle null pointer exceptions
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(NullPointerException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", ErrorCode.INTERNAL_ERROR.getCode()));
    }

    /**
     * Handle no handler found exceptions (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found", ErrorCode.NOT_FOUND.getCode()));
    }

    /**
     * Handle security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleSecurityException(SecurityException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", ErrorCode.FORBIDDEN.getCode()));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        // Log the exception (you might want to use a logger here)
        System.err.println("Unhandled exception: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", ErrorCode.INTERNAL_ERROR.getCode()));
    }

    /**
     * Handle custom business exceptions (if you create them)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Map business exception types to HTTP status codes
        if (ex.getType() == BusinessException.Type.NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getType() == BusinessException.Type.FORBIDDEN) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex.getType() == BusinessException.Type.UNAUTHORIZED) {
            status = HttpStatus.UNAUTHORIZED;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().getCode()));
    }
}