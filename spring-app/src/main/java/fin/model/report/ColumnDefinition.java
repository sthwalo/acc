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

package fin.model.report;

/**
 * Defines column metadata for report exports (PDF, Excel, CSV).
 * Used by ReportExportService to generate properly formatted reports.
 * 
 * @see fin.service.export.ReportExportService
 */
public class ColumnDefinition {
    
    private final String headerName;
    private final String fieldName;
    private final int width;
    private final String format;
    private final String alignment;
    
    /**
     * Creates a column definition with full specification
     *
     * @param headerName Display name for column header (e.g., "Account Code")
     * @param fieldName Field key in data map (e.g., "accountCode")
     * @param width Column width in points/pixels
     * @param format Data format: "text", "currency", "date", "number"
     * @param alignment Text alignment: "left", "center", "right"
     */
    public ColumnDefinition(String headerName, String fieldName, int width, String format, String alignment) {
        this.headerName = headerName;
        this.fieldName = fieldName;
        this.width = width;
        this.format = format;
        this.alignment = alignment;
    }
    
    /**
     * Creates a column definition with default format (text) and alignment (left)
     *
     * @param headerName Display name for column header
     * @param fieldName Field key in data map
     * @param width Column width in points/pixels
     */
    public ColumnDefinition(String headerName, String fieldName, int width) {
        this(headerName, fieldName, width, "text", "left");
    }
    
    public String getHeaderName() {
        return headerName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public int getWidth() {
        return width;
    }
    
    public String getFormat() {
        return format;
    }
    
    public String getAlignment() {
        return alignment;
    }
    
    @Override
    public String toString() {
        return "ColumnDefinition{" +
                "headerName='" + headerName + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", width=" + width +
                ", format='" + format + '\'' +
                ", alignment='" + alignment + '\'' +
                '}';
    }
}
