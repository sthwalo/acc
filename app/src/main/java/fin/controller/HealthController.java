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

package fin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Health check operations
 * Provides system health status endpoints
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Health check endpoint - no authentication required
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        String databaseStatus = "disconnected";
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) { // 5 second timeout
                databaseStatus = "connected";
            }
        } catch (SQLException e) {
            // Database connection failed, status remains "disconnected"
        }

        HealthResponse response = new HealthResponse(
            "healthy",
            databaseStatus,
            System.currentTimeMillis()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get current system time
     */
    @GetMapping("/time")
    public ResponseEntity<SystemTimeResponse> getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String fiscalPeriod = calculateCurrentFiscalPeriod(now.toLocalDate());

        SystemTimeResponse response = new SystemTimeResponse(formattedTime, fiscalPeriod);
        return ResponseEntity.ok(response);
    }

    /**
     * Get system logs
     */
    @GetMapping("/logs")
    public ResponseEntity<SystemLogsResponse> getSystemLogs(
            @RequestParam(defaultValue = "application") String type,
            @RequestParam(defaultValue = "50") int lines) {

        List<String> logs = new ArrayList<>();

        try {
            String logFilePath = getLogFilePath(type);
            File logFile = new File(logFilePath);

            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    List<String> allLines = new ArrayList<>();

                    while ((line = reader.readLine()) != null) {
                        allLines.add(line);
                    }

                    // Get the last 'lines' number of log entries
                    int startIndex = Math.max(0, allLines.size() - lines);
                    for (int i = startIndex; i < allLines.size(); i++) {
                        logs.add(allLines.get(i));
                    }
                }
            } else {
                logs.add("Log file not found: " + logFilePath);
            }

        } catch (IOException e) {
            logs.add("Error reading log file: " + e.getMessage());
        }

        SystemLogsResponse response = new SystemLogsResponse(logs, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Database diagnostics endpoint
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<DatabaseDiagnosticsResponse> diagnostics() {
        int industriesCount = 0;
        int ruleTemplatesCount = 0;
        int companiesWithIndustryCount = 0;
        int totalCompaniesCount = 0;

        try (Connection connection = dataSource.getConnection()) {
            // Check industries count
            try (var industriesStmt = connection.prepareStatement("SELECT COUNT(*) FROM industries");
                 var industriesResult = industriesStmt.executeQuery()) {
                if (industriesResult.next()) {
                    industriesCount = industriesResult.getInt(1);
                }
            }

            // Check rule templates count
            try (var templatesStmt = connection.prepareStatement("SELECT COUNT(*) FROM rule_templates");
                 var templatesResult = templatesStmt.executeQuery()) {
                if (templatesResult.next()) {
                    ruleTemplatesCount = templatesResult.getInt(1);
                }
            }

            // Check companies with industries
            try (var companiesStmt = connection.prepareStatement("SELECT COUNT(*) FROM companies WHERE industry_id IS NOT NULL");
                 var companiesResult = companiesStmt.executeQuery()) {
                if (companiesResult.next()) {
                    companiesWithIndustryCount = companiesResult.getInt(1);
                }
            }

            // Check total companies
            try (var totalCompaniesStmt = connection.prepareStatement("SELECT COUNT(*) FROM companies");
                 var totalCompaniesResult = totalCompaniesStmt.executeQuery()) {
                if (totalCompaniesResult.next()) {
                    totalCompaniesCount = totalCompaniesResult.getInt(1);
                }
            }

            DatabaseDiagnosticsResponse response = new DatabaseDiagnosticsResponse(
                "success",
                null,
                industriesCount,
                ruleTemplatesCount,
                companiesWithIndustryCount,
                totalCompaniesCount
            );

            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            DatabaseDiagnosticsResponse response = new DatabaseDiagnosticsResponse(
                "error",
                e.getMessage(),
                industriesCount,
                ruleTemplatesCount,
                companiesWithIndustryCount,
                totalCompaniesCount
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Calculate current fiscal period based on date
     */
    private String calculateCurrentFiscalPeriod(java.time.LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        // Assuming fiscal year starts in March (March 1st to February 28/29)
        if (month >= 3) {
            return String.format("FY%d-%d", year, year + 1);
        } else {
            return String.format("FY%d-%d", year - 1, year);
        }
    }

    /**
     * Get log file path based on type
     */
    private String getLogFilePath(String type) {
        String baseDir = System.getProperty("user.dir");
        switch (type.toLowerCase()) {
            case "application":
                return baseDir + "/logs/application.log";
            case "database":
                return baseDir + "/logs/database.log";
            case "error":
                return baseDir + "/logs/error.log";
            default:
                return baseDir + "/logs/application.log";
        }
    }

    /**
     * Health response DTO
     */
    public static class HealthResponse {
        private final String status;
        private final String database;
        private final long timestamp;

        public HealthResponse(String status, String database, long timestamp) {
            this.status = status;
            this.database = database;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public String getDatabase() { return database; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * System time response DTO
     */
    public static class SystemTimeResponse {
        private final String currentTime;
        private final String fiscalPeriod;

        public SystemTimeResponse(String currentTime, String fiscalPeriod) {
            this.currentTime = currentTime;
            this.fiscalPeriod = fiscalPeriod;
        }

        public String getCurrentTime() { return currentTime; }
        public String getFiscalPeriod() { return fiscalPeriod; }
    }

    /**
     * System logs response DTO
     */
    public static class SystemLogsResponse {
        private final List<String> logs;
        private final String logType;

        public SystemLogsResponse(List<String> logs, String logType) {
            this.logs = logs;
            this.logType = logType;
        }

        public List<String> getLogs() { return logs; }
        public String getLogType() { return logType; }
    }

    /**
     * Database diagnostics response DTO
     */
    public static class DatabaseDiagnosticsResponse {
        private final String status;
        private final String error;
        private final int industriesCount;
        private final int ruleTemplatesCount;
        private final int companiesWithIndustryCount;
        private final int totalCompaniesCount;

        public DatabaseDiagnosticsResponse(String status, String error, int industriesCount, int ruleTemplatesCount,
                                           int companiesWithIndustryCount, int totalCompaniesCount) {
            this.status = status;
            this.error = error;
            this.industriesCount = industriesCount;
            this.ruleTemplatesCount = ruleTemplatesCount;
            this.companiesWithIndustryCount = companiesWithIndustryCount;
            this.totalCompaniesCount = totalCompaniesCount;
        }

        public String getStatus() { return status; }
        public String getError() { return error; }
        public int getIndustriesCount() { return industriesCount; }
        public int getRuleTemplatesCount() { return ruleTemplatesCount; }
        public int getCompaniesWithIndustryCount() { return companiesWithIndustryCount; }
        public int getTotalCompaniesCount() { return totalCompaniesCount; }
    }
}