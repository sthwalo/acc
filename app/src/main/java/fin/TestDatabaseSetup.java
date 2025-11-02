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

package fin;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test Database Setup utility for CI/CD pipeline
 *
 * This class is used by the CI/CD pipeline to set up the test database
 * before running tests. It loads the test schema and initializes test data.
 */
public class TestDatabaseSetup {

    public static void main(String[] args) {
        String dbUrl = System.getProperty("TEST_DATABASE_URL", System.getenv("TEST_DATABASE_URL"));
        String dbUser = System.getProperty("TEST_DATABASE_USER", System.getenv("TEST_DATABASE_USER"));
        String dbPassword = System.getProperty("TEST_DATABASE_PASSWORD", System.getenv("TEST_DATABASE_PASSWORD"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("❌ Missing database configuration. Please set TEST_DATABASE_URL, TEST_DATABASE_USER, and TEST_DATABASE_PASSWORD");
            System.exit(1);
        }

        try {
            System.out.println("🗄️ Starting test database setup for CI/CD...");
            System.out.println("📋 Database URL: " + dbUrl);
            System.out.println("👤 Database User: " + dbUser);

            setupTestDatabase(dbUrl, dbUser, dbPassword);

            System.out.println("✅ Test database setup completed successfully!");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ Test database setup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void setupTestDatabase(String dbUrl, String dbUser, String dbPassword) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Disable auto-commit to control transactions
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {

            // Load the test schema
            String schemaPath = "app/src/test/resources/test_schema.sql";
            String schemaSql = Files.readString(Paths.get(schemaPath));

            System.out.println("📄 Loaded schema from: " + schemaPath);
            System.out.println("🔧 Executing database schema...");

            // Parse and execute SQL statements properly
            // pg_dump format has multi-line statements - we need to split on semicolons
            // but respect line continuations and SQL structure
            StringBuilder currentStatement = new StringBuilder();
            boolean inFunction = false;
            int successCount = 0;
            int skipCount = 0;

            for (String line : schemaSql.split("\n")) {
                String trimmed = line.trim();
                
                // Track if we're inside a function definition
                if (trimmed.contains("$$")) {
                    inFunction = !inFunction;
                }
                
                // Skip pure comment lines and empty lines at statement boundaries
                if (currentStatement.length() == 0 && (trimmed.isEmpty() || trimmed.startsWith("--"))) {
                    continue;
                }
                
                currentStatement.append(line).append("\n");
                
                // Execute when we hit a semicolon (unless inside a function)
                if (trimmed.endsWith(";") && !inFunction) {
                    String sql = currentStatement.toString().trim();
                    currentStatement.setLength(0);
                    
                    if (sql.isEmpty() || sql.startsWith("--")) {
                        continue;
                    }
                    
                    try {
                        stmt.execute(sql);
                        successCount++;
                        // Log CREATE TABLE statements for debugging
                        if (sql.toUpperCase().startsWith("CREATE TABLE")) {
                            String tableName = sql.substring(13, Math.min(50, sql.length())).split("\\s+")[0];
                            System.out.println("  ✓ Created table: " + tableName);
                        }
                    } catch (Exception e) {
                        // Ignore errors for DROP statements on non-existent objects
                        String errorMsg = e.getMessage().toLowerCase();
                        boolean isDrop = sql.toUpperCase().startsWith("DROP");
                        boolean isAlter = sql.toUpperCase().startsWith("ALTER");
                        boolean isDoesNotExist = errorMsg.contains("does not exist");
                        
                        if ((isDrop || isAlter) && isDoesNotExist) {
                            // Silently skip - expected for initial setup
                            skipCount++;
                        } else {
                            // Log unexpected errors
                            System.err.println("⚠️ Error: " + e.getMessage());
                            System.err.println("   SQL: " + sql.substring(0, Math.min(100, sql.length())));
                            skipCount++;
                        }
                    }
                }
            }

            System.out.println("✅ Schema executed successfully (" + successCount + " statements, " + skipCount + " skipped)");

                // Commit the schema changes before inserting data
                conn.commit();
                System.out.println("✅ Schema committed to database");

                // Insert basic test data
                insertTestData(stmt);
                
                // Commit the test data
                conn.commit();
            }
        }
    }

    private static void insertTestData(Statement stmt) throws Exception {
        System.out.println("📝 Inserting test data...");

        // Set search_path to public schema
        stmt.execute("SET search_path TO public");

        // Insert account types
        stmt.executeUpdate("""
            INSERT INTO public.account_types (code, name, normal_balance, description) VALUES
            ('ASSET', 'Asset', 'D', 'Assets - resources owned by the company'),
            ('LIAB', 'Liability', 'C', 'Liabilities - debts owed by the company'),
            ('EQUITY', 'Equity', 'C', 'Owner equity in the company'),
            ('REV', 'Revenue', 'C', 'Revenue earned by the company'),
            ('EXP', 'Expense', 'D', 'Expenses incurred by the company')
            ON CONFLICT (code) DO NOTHING
        """);

        // Insert test company
        stmt.executeUpdate("""
            INSERT INTO public.companies (id, name, registration_number, tax_number, address, contact_email, contact_phone)
            VALUES (1, 'Test Company Ltd', '2023/123456/07', '9876543210', '123 Test St, Test City', 'test@example.com', '+27-11-123-4567')
            ON CONFLICT (id) DO NOTHING
        """);

        // Insert test fiscal period
        stmt.executeUpdate("""
            INSERT INTO public.fiscal_periods (id, company_id, period_name, start_date, end_date, is_closed)
            VALUES (1, 1, '2025 Financial Year', '2025-01-01', '2025-12-31', false)
            ON CONFLICT (id) DO NOTHING
        """);

        System.out.println("✅ Test data inserted successfully");
    }
}