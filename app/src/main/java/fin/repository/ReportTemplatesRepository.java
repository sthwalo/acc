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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Repository for report templates stored in database-first architecture
 */
public class ReportTemplatesRepository {
    private static final Logger LOGGER = Logger.getLogger(ReportTemplatesRepository.class.getName());
    private final String dbUrl;

    public ReportTemplatesRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Get report template text by company, type, and key
     */
    public String getTemplateText(Long companyId, String templateType, String templateKey) throws SQLException {
        String sql = """
            SELECT template_text FROM report_templates
            WHERE company_id = ? AND template_type = ? AND template_key = ? AND is_active = TRUE
            ORDER BY display_order ASC LIMIT 1
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, companyId);
            pstmt.setString(2, templateType);
            pstmt.setString(3, templateKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("template_text");
                }
            }
        }

        throw new SQLException("Report template not found for company " + companyId +
            ", type '" + templateType + "', key '" + templateKey + "'. " +
            "Please insert data into report_templates table: " +
            "INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order) " +
            "VALUES (" + companyId + ", '" + templateType + "', '" + templateKey + "', '[template text]', 1)");
    }

    /**
     * Get all templates for a specific type and company
     */
    public List<ReportTemplate> getTemplatesByType(Long companyId, String templateType) throws SQLException {
        String sql = """
            SELECT template_key, template_text, display_order
            FROM report_templates
            WHERE company_id = ? AND template_type = ? AND is_active = TRUE
            ORDER BY display_order ASC
            """;

        List<ReportTemplate> templates = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, companyId);
            pstmt.setString(2, templateType);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReportTemplate template = new ReportTemplate();
                    template.templateKey = rs.getString("template_key");
                    template.templateText = rs.getString("template_text");
                    template.displayOrder = rs.getInt("display_order");
                    templates.add(template);
                }
            }
        }

        if (templates.isEmpty()) {
            throw new SQLException("No report templates found for company " + companyId +
                ", type '" + templateType + "'. Please insert data into report_templates table.");
        }

        return templates;
    }

    public static class ReportTemplate {
        public String templateKey;
        public String templateText;
        public int displayOrder;
    }
}