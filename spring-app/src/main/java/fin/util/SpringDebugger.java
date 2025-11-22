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

package fin.util;

import org.springframework.stereotype.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Centralized debugging utility for Spring Boot application.
 * Provides comprehensive exception logging, stack trace analysis,
 * and debugging information for all services, controllers, and repositories.
 */
@Component
public class SpringDebugger {

    private static final Logger LOGGER = Logger.getLogger(SpringDebugger.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Log an exception with full context and stack trace
     */
    public void logException(String operation, String className, String methodName, Exception e, Object... params) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append("=".repeat(80)).append("\n");
        sb.append("🔴 EXCEPTION DETECTED - ").append(timestamp).append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append("Operation: ").append(operation).append("\n");
        sb.append("Class: ").append(className).append("\n");
        sb.append("Method: ").append(methodName).append("\n");

        if (params != null && params.length > 0) {
            sb.append("Parameters: ").append(formatParameters(params)).append("\n");
        }

        sb.append("Exception Type: ").append(e.getClass().getSimpleName()).append("\n");
        sb.append("Exception Message: ").append(e.getMessage()).append("\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append("FULL STACK TRACE:\n");

        // Add full stack trace
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }

        // Add cause if present
        Throwable cause = e.getCause();
        if (cause != null) {
            sb.append("-".repeat(80)).append("\n");
            sb.append("CAUSED BY: ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        }

        sb.append("=".repeat(80)).append("\n");

        LOGGER.log(Level.SEVERE, sb.toString());
    }

    /**
     * Log method entry with parameters
     */
    public void logMethodEntry(String className, String methodName, Object... params) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        StringBuilder sb = new StringBuilder();

        sb.append("▶️  METHOD ENTRY - ").append(timestamp).append(" - ");
        sb.append(className).append(".").append(methodName);

        if (params != null && params.length > 0) {
            sb.append(" (").append(formatParameters(params)).append(")");
        }

        LOGGER.info(sb.toString());
    }

    /**
     * Log method exit with result
     */
    public void logMethodExit(String className, String methodName, Object result) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String resultStr = result != null ? result.toString() : "null";

        // Truncate result if too long
        if (resultStr.length() > 200) {
            resultStr = resultStr.substring(0, 200) + "...";
        }

        LOGGER.info("◀️  METHOD EXIT - " + timestamp + " - " + className + "." + methodName + " → " + resultStr);
    }

    /**
     * Log method exit with timing information
     */
    public void logMethodExitWithTiming(String className, String methodName, long startTime, Object result) {
        long duration = System.currentTimeMillis() - startTime;
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String resultStr = result != null ? result.toString() : "null";

        // Truncate result if too long
        if (resultStr.length() > 200) {
            resultStr = resultStr.substring(0, 200) + "...";
        }

        String timing = duration < 1000 ? duration + "ms" : (duration / 1000.0) + "s";

        LOGGER.info("◀️  METHOD EXIT - " + timestamp + " - " + className + "." + methodName +
                   " → " + resultStr + " [" + timing + "]");
    }

    /**
     * Log database operation
     */
    public void logDatabaseOperation(String operation, String table, String conditions, int affectedRows) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        LOGGER.info("🗄️  DB OPERATION - " + timestamp + " - " + operation + " on " + table +
                   (conditions != null ? " WHERE " + conditions : "") + " → " + affectedRows + " rows");
    }

    /**
     * Log validation error
     */
    public void logValidationError(String className, String methodName, String field, String value, String error) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        LOGGER.warning("⚠️  VALIDATION ERROR - " + timestamp + " - " + className + "." + methodName +
                      " - Field: " + field + " = '" + value + "' - " + error);
    }

    /**
     * Log business rule violation
     */
    public void logBusinessRuleViolation(String ruleName, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        LOGGER.warning("🚫 BUSINESS RULE VIOLATION - " + timestamp + " - " + ruleName + ": " + details);
    }

    /**
     * Log performance warning
     */
    public void logPerformanceWarning(String operation, long durationMs, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String timing = durationMs < 1000 ? durationMs + "ms" : (durationMs / 1000.0) + "s";

        LOGGER.warning("🐌 PERFORMANCE WARNING - " + timestamp + " - " + operation +
                      " took " + timing + " - " + details);
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String user, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        LOGGER.info("🔐 SECURITY EVENT - " + timestamp + " - " + event +
                   " - User: " + user + " - " + details);
    }

    /**
     * Create a method execution timer
     */
    public MethodTimer startTimer(String className, String methodName) {
        return new MethodTimer(className, methodName);
    }

    /**
     * Format parameters for logging
     */
    private String formatParameters(Object... params) {
        return Arrays.stream(params)
                .map(param -> param != null ? param.toString() : "null")
                .map(str -> str.length() > 50 ? str.substring(0, 50) + "..." : str)
                .collect(Collectors.joining(", "));
    }

    /**
     * Inner class for timing method execution
     */
    public static class MethodTimer {
        private final String className;
        private final String methodName;
        private final long startTime;

        public MethodTimer(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.startTime = System.currentTimeMillis();
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        public void logExit(Object result) {
            SpringDebugger debugger = new SpringDebugger();
            debugger.logMethodExitWithTiming(className, methodName, startTime, result);
        }
    }
}