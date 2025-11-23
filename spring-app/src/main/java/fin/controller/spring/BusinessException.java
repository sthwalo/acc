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

/**
 * Custom business exception for domain-specific errors.
 * Allows controllers to throw business-specific exceptions that get
 * properly mapped to appropriate HTTP responses.
 */
public class BusinessException extends RuntimeException {

    public enum Type {
        NOT_FOUND,
        VALIDATION_ERROR,
        BUSINESS_RULE_VIOLATION,
        FORBIDDEN,
        UNAUTHORIZED,
        CONFLICT
    }

    private final Type type;
    private final ErrorCode errorCode;

    public BusinessException(String message, Type type, ErrorCode errorCode) {
        super(message);
        this.type = type;
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Type type, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.errorCode = errorCode;
    }

    public Type getType() {
        return type;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    // Factory methods for common business exceptions

    public static BusinessException notFound(String resource) {
        return new BusinessException(
            resource + " not found",
            Type.NOT_FOUND,
            ErrorCode.NOT_FOUND
        );
    }

    public static BusinessException validationError(String message) {
        return new BusinessException(
            message,
            Type.VALIDATION_ERROR,
            ErrorCode.VALIDATION_ERROR
        );
    }

    public static BusinessException businessRuleViolation(String message) {
        return new BusinessException(
            message,
            Type.BUSINESS_RULE_VIOLATION,
            ErrorCode.BUSINESS_RULE_VIOLATION
        );
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(
            message,
            Type.FORBIDDEN,
            ErrorCode.FORBIDDEN
        );
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(
            message,
            Type.UNAUTHORIZED,
            ErrorCode.UNAUTHORIZED
        );
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(
            message,
            Type.CONFLICT,
            ErrorCode.INVALID_OPERATION
        );
    }
}