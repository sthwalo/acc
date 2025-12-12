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

/**
 * Standardized error codes for API responses.
 * Ensures consistent error handling across frontend and backend.
 */
public enum ErrorCode {

    // General errors
    INTERNAL_ERROR("INTERNAL_ERROR", "An internal server error occurred"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Input validation failed"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("FORBIDDEN", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),

    // Company-related errors
    COMPANY_NOT_FOUND("COMPANY_NOT_FOUND", "Company not found"),
    COMPANY_ALREADY_EXISTS("COMPANY_ALREADY_EXISTS", "Company already exists"),
    COMPANY_INACTIVE("COMPANY_INACTIVE", "Company is inactive"),

    // Fiscal period errors
    FISCAL_PERIOD_NOT_FOUND("FISCAL_PERIOD_NOT_FOUND", "Fiscal period not found"),
    FISCAL_PERIOD_ALREADY_EXISTS("FISCAL_PERIOD_ALREADY_EXISTS", "Fiscal period already exists"),
    FISCAL_PERIOD_CLOSED("FISCAL_PERIOD_CLOSED", "Fiscal period is closed"),
    INVALID_FISCAL_PERIOD_DATES("INVALID_FISCAL_PERIOD_DATES", "Invalid fiscal period dates"),

    // Transaction errors
    TRANSACTION_NOT_FOUND("TRANSACTION_NOT_FOUND", "Transaction not found"),
    NO_TRANSACTIONS_FOUND("NO_TRANSACTIONS_FOUND", "No transactions found for the selected criteria"),
    TRANSACTION_PROCESSING_FAILED("TRANSACTION_PROCESSING_FAILED", "Failed to process transaction"),
    INVALID_TRANSACTION_DATA("INVALID_TRANSACTION_DATA", "Invalid transaction data"),

    // Bank statement errors
    BANK_STATEMENT_NOT_FOUND("BANK_STATEMENT_NOT_FOUND", "Bank statement not found"),
    BANK_STATEMENT_PROCESSING_FAILED("BANK_STATEMENT_PROCESSING_FAILED", "Failed to process bank statement"),
    INVALID_BANK_STATEMENT_FORMAT("INVALID_BANK_STATEMENT_FORMAT", "Invalid bank statement format"),

    // Classification errors
    CLASSIFICATION_FAILED("CLASSIFICATION_FAILED", "Transaction classification failed"),
    CLASSIFICATION_RULE_NOT_FOUND("CLASSIFICATION_RULE_NOT_FOUND", "Classification rule not found"),

    // Database errors
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed"),
    CONNECTION_ERROR("CONNECTION_ERROR", "Database connection failed"),

    // File processing errors
    FILE_NOT_FOUND("FILE_NOT_FOUND", "File not found"),
    FILE_PROCESSING_FAILED("FILE_PROCESSING_FAILED", "File processing failed"),
    INVALID_FILE_FORMAT("INVALID_FILE_FORMAT", "Invalid file format"),

    // Business logic errors
    INSUFFICIENT_PERMISSIONS("INSUFFICIENT_PERMISSIONS", "Insufficient permissions"),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "Business rule violation"),
    INVALID_OPERATION("INVALID_OPERATION", "Invalid operation for current state");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Get error code from string
     */
    public static ErrorCode fromString(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}