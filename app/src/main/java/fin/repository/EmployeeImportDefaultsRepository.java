/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
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

package fin.repository;

import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Repository for employee import defaults stored in database-first architecture
 */
public class EmployeeImportDefaultsRepository {
    private static final Logger LOGGER = Logger.getLogger(EmployeeImportDefaultsRepository.class.getName());
    private final String dbUrl;

    public EmployeeImportDefaultsRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Get default country code for employee imports
     */
    public String getDefaultCountryCode(Long companyId) throws SQLException {
        String sql = """
            SELECT default_value FROM employee_import_defaults
            WHERE company_id = ? AND field_name = 'country_code'
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, companyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("default_value");
                }
            }
        }

        throw new SQLException("Employee import default not found for company " + companyId +
            ", field 'country_code'. Please insert data into employee_import_defaults table: " +
            "INSERT INTO employee_import_defaults (company_id, field_name, default_value, is_required) " +
            "VALUES (" + companyId + ", 'country_code', 'ZA', TRUE)");
    }

    /**
     * Get default value for any employee import field
     */
    public String getDefaultValue(Long companyId, String fieldName) throws SQLException {
        String sql = """
            SELECT default_value FROM employee_import_defaults
            WHERE company_id = ? AND field_name = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, companyId);
            pstmt.setString(2, fieldName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("default_value");
                }
            }
        }

        throw new SQLException("Employee import default not found for company " + companyId +
            ", field '" + fieldName + "'. Please insert data into employee_import_defaults table: " +
            "INSERT INTO employee_import_defaults (company_id, field_name, default_value, is_required) " +
            "VALUES (" + companyId + ", '" + fieldName + "', '[default value]', TRUE)");
    }
}