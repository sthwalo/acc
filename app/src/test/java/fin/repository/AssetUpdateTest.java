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

package fin.repository;

import fin.TestConfiguration;
import fin.model.Asset;
import fin.service.DepreciationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to debug asset update issue
 */
public class AssetUpdateTest {
    private DepreciationService service;

    @BeforeAll
    public static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();

        // Create assets table and insert test data
        try (Connection conn = DriverManager.getConnection(TestConfiguration.TEST_DB_URL_WITH_CREDENTIALS);
             Statement stmt = conn.createStatement()) {

            // Drop table if it exists to ensure clean schema
            stmt.executeUpdate("DROP TABLE IF EXISTS assets CASCADE");

            // Create assets table with all required columns
            stmt.executeUpdate("""
                CREATE TABLE assets (
                    id BIGINT PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    asset_code VARCHAR(50) NOT NULL UNIQUE,
                    asset_name VARCHAR(255) NOT NULL,
                    description TEXT,
                    asset_category VARCHAR(100),
                    acquisition_date DATE NOT NULL,
                    cost DECIMAL(15,2) NOT NULL,
                    salvage_value DECIMAL(15,2) DEFAULT 0,
                    useful_life_years INTEGER NOT NULL,
                    location VARCHAR(255),
                    department VARCHAR(255),
                    status VARCHAR(50) DEFAULT 'ACTIVE',
                    accumulated_depreciation DECIMAL(15,2) DEFAULT 0,
                    created_by VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
                )
                """);

            // Insert test asset with ID 11
            stmt.executeUpdate("""
                INSERT INTO assets (id, company_id, asset_code, asset_name, description, asset_category, acquisition_date, cost, salvage_value, useful_life_years, location, department, status, created_by)
                VALUES (11, 1, 'COMP001', 'Test Computer', 'Test computer for depreciation', 'COMPUTER_EQUIPMENT', '2023-01-01', 10000.00, 1000.00, 5, 'Main Office', 'IT Department', 'ACTIVE', 'test_user')
                """);

            System.out.println("✅ Assets table created and test data inserted");
        }
    }

    @BeforeEach
    public void setUp() {
        DepreciationRepository repo = new DepreciationRepository(TestConfiguration.TEST_DB_URL_WITH_CREDENTIALS);
        service = new DepreciationService(repo);
    }

    @Test
    public void testAssetUpdate() {
        // Test updating asset ID 11
        System.out.println("Testing asset update for ID 11...");

        var assetOpt = service.getAssetById(11L);
        assertTrue(assetOpt.isPresent(), "Asset 11 should exist");

        Asset asset = assetOpt.get();
        System.out.println("Found asset: ID=" + asset.getId() + ", code='" + asset.getAssetCode() + "', name='" + asset.getAssetName() + "', companyId=" + asset.getCompanyId());

        // Try to update the salvage value
        BigDecimal originalSalvageValue = asset.getSalvageValue();
        BigDecimal newSalvageValue = originalSalvageValue.add(BigDecimal.ONE);
        System.out.println("Updating salvage value from " + originalSalvageValue + " to " + newSalvageValue);

        asset.setSalvageValue(newSalvageValue);

        // This should trigger the debug output in updateAsset
        assertDoesNotThrow(() -> service.saveAsset(asset), "Asset update should not throw exception");

        // Verify the update worked
        var updatedAssetOpt = service.getAssetById(11L);
        assertTrue(updatedAssetOpt.isPresent(), "Asset should still exist after update");
        Asset updatedAsset = updatedAssetOpt.get();
        assertEquals(newSalvageValue, updatedAsset.getSalvageValue(), "Salvage value should be updated");

        System.out.println("Asset update test successful!");
    }
}