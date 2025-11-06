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

import fin.model.Asset;
import fin.model.DepreciationMethod;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;
import fin.model.Disposal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing depreciation data in the database
 */
public class DepreciationRepository {
    // SQL parameter indices for depreciation_schedules insert
    private static final int SCHEDULE_COMPANY_ID_PARAM = 1;
    private static final int SCHEDULE_ASSET_ID_PARAM = 2;
    private static final int SCHEDULE_NUMBER_PARAM = 3;
    private static final int SCHEDULE_NAME_PARAM = 4;
    private static final int SCHEDULE_DESCRIPTION_PARAM = 5;
    private static final int SCHEDULE_COST_PARAM = 6;
    private static final int SCHEDULE_SALVAGE_VALUE_PARAM = 7;
    private static final int SCHEDULE_USEFUL_LIFE_PARAM = 8;
    private static final int SCHEDULE_METHOD_PARAM = 9;
    private static final int SCHEDULE_DB_FACTOR_PARAM = 10;
    private static final int SCHEDULE_CONVENTION_PARAM = 11;
    private static final int SCHEDULE_TOTAL_DEPR_PARAM = 12;
    private static final int SCHEDULE_FINAL_BOOK_VALUE_PARAM = 13;
    private static final int SCHEDULE_STATUS_PARAM = 14;
    private static final int SCHEDULE_CREATED_BY_PARAM = 15;

    // SQL parameter indices for depreciation_entries insert
    private static final int ENTRY_SCHEDULE_ID_PARAM = 1;
    private static final int ENTRY_YEAR_NUMBER_PARAM = 2;
    private static final int ENTRY_FISCAL_YEAR_PARAM = 3;
    private static final int ENTRY_PERIOD_START_PARAM = 4;
    private static final int ENTRY_PERIOD_END_PARAM = 5;
    private static final int ENTRY_DEPR_AMOUNT_PARAM = 6;
    private static final int ENTRY_CUMULATIVE_DEPR_PARAM = 7;
    private static final int ENTRY_BOOK_VALUE_PARAM = 8;

    // SQL parameter indices for assets insert
    private static final int ASSET_COMPANY_ID_PARAM = 1;
    private static final int ASSET_CODE_PARAM = 2;
    private static final int ASSET_NAME_PARAM = 3;
    private static final int ASSET_DESCRIPTION_PARAM = 4;
    private static final int ASSET_CATEGORY_PARAM = 5;
    private static final int ASSET_ACQUISITION_DATE_PARAM = 6;
    private static final int ASSET_COST_PARAM = 7;
    private static final int ASSET_SALVAGE_VALUE_PARAM = 8;
    private static final int ASSET_USEFUL_LIFE_PARAM = 9;
    private static final int ASSET_LOCATION_PARAM = 10;
    private static final int ASSET_DEPARTMENT_PARAM = 11;
    private static final int ASSET_STATUS_PARAM = 12;
    private static final int ASSET_CREATED_BY_PARAM = 13;

    private final String dbUrl;

    public DepreciationRepository(String databaseUrl) {
        this.dbUrl = databaseUrl;
    }

    /**
     * Save a depreciation schedule and its entries
     */
    public DepreciationSchedule saveDepreciationSchedule(DepreciationSchedule schedule, Long companyId) throws SQLException {
        String sql = """
            INSERT INTO depreciation_schedules (
                company_id, asset_id, schedule_number, schedule_name, description,
                cost, salvage_value, useful_life_years, depreciation_method,
                db_factor, convention, total_depreciation, final_book_value,
                status, created_by, journal_entry_id, posted_at, posted_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(SCHEDULE_COMPANY_ID_PARAM, companyId);
            stmt.setObject(SCHEDULE_ASSET_ID_PARAM, schedule.getAssetId());
            stmt.setString(SCHEDULE_NUMBER_PARAM, generateScheduleNumber(companyId));
            stmt.setString(SCHEDULE_NAME_PARAM, schedule.getScheduleName());
            stmt.setString(SCHEDULE_DESCRIPTION_PARAM, schedule.getDescription());
            stmt.setBigDecimal(SCHEDULE_COST_PARAM, schedule.getCost());
            stmt.setBigDecimal(SCHEDULE_SALVAGE_VALUE_PARAM, schedule.getSalvageValue());
            stmt.setInt(SCHEDULE_USEFUL_LIFE_PARAM, schedule.getUsefulLifeYears());
            stmt.setString(SCHEDULE_METHOD_PARAM, schedule.getDepreciationMethod().name());
            stmt.setBigDecimal(SCHEDULE_DB_FACTOR_PARAM, schedule.getDbFactor());
            stmt.setString(SCHEDULE_CONVENTION_PARAM, schedule.getConvention());
            stmt.setBigDecimal(SCHEDULE_TOTAL_DEPR_PARAM, schedule.getTotalDepreciation());
            stmt.setBigDecimal(SCHEDULE_FINAL_BOOK_VALUE_PARAM, schedule.getFinalBookValue());
            stmt.setString(SCHEDULE_STATUS_PARAM, schedule.getStatus());
            stmt.setString(SCHEDULE_CREATED_BY_PARAM, schedule.getCreatedBy());
            stmt.setObject(16, schedule.getJournalEntryId());
            stmt.setTimestamp(17, schedule.getPostedAt() != null ? Timestamp.valueOf(schedule.getPostedAt()) : null);
            stmt.setString(18, schedule.getPostedBy());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long scheduleId = rs.getLong(1);
                schedule.setId(scheduleId);

                // Get the asset to retrieve acquisition date for period calculations
                Optional<Asset> assetOpt = findAssetById(schedule.getAssetId());
                if (assetOpt.isEmpty()) {
                    throw new SQLException("Asset not found with ID: " + schedule.getAssetId());
                }
                LocalDate acquisitionDate = assetOpt.get().getAcquisitionDate();

                // Save the depreciation entries
                saveDepreciationEntries(scheduleId, schedule.getYears(), acquisitionDate, conn);

                return schedule;
            }
        }

        throw new SQLException("Failed to save depreciation schedule");
    }

    /**
     * Save depreciation entries for a schedule
     */
    private void saveDepreciationEntries(Long scheduleId, List<DepreciationYear> years, LocalDate acquisitionDate, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO depreciation_entries (
                depreciation_schedule_id, year_number, fiscal_year,
                period_start, period_end, depreciation_amount,
                cumulative_depreciation, book_value
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (DepreciationYear year : years) {
                stmt.setLong(ENTRY_SCHEDULE_ID_PARAM, scheduleId);
                stmt.setInt(ENTRY_YEAR_NUMBER_PARAM, year.getYear());
                stmt.setInt(ENTRY_FISCAL_YEAR_PARAM, calculateFiscalYear(acquisitionDate, year.getYear()));
                stmt.setDate(ENTRY_PERIOD_START_PARAM, Date.valueOf(getFiscalYearStart(acquisitionDate, year.getYear())));
                stmt.setDate(ENTRY_PERIOD_END_PARAM, Date.valueOf(getFiscalYearEnd(acquisitionDate, year.getYear())));
                stmt.setBigDecimal(ENTRY_DEPR_AMOUNT_PARAM, year.getDepreciation());
                stmt.setBigDecimal(ENTRY_CUMULATIVE_DEPR_PARAM, year.getCumulativeDepreciation());
                stmt.setBigDecimal(ENTRY_BOOK_VALUE_PARAM, year.getBookValue());

                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Find depreciation schedules for a company
     */
    public List<DepreciationSchedule> findSchedulesByCompany(Long companyId) throws SQLException {
        String sql = """
            SELECT id, schedule_number, schedule_name, cost, salvage_value,
                   useful_life_years, depreciation_method, total_depreciation,
                   final_book_value, status, calculation_date
            FROM depreciation_schedules
            WHERE company_id = ?
            ORDER BY calculation_date DESC
            """;

        List<DepreciationSchedule> schedules = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                DepreciationSchedule schedule = new DepreciationSchedule();
                schedule.setId(rs.getLong("id"));
                schedule.setScheduleNumber(rs.getString("schedule_number"));
                schedule.setScheduleName(rs.getString("schedule_name"));
                schedule.setCost(rs.getBigDecimal("cost"));
                schedule.setSalvageValue(rs.getBigDecimal("salvage_value"));
                schedule.setUsefulLifeYears(rs.getInt("useful_life_years"));
                schedule.setDepreciationMethod(DepreciationMethod.valueOf(rs.getString("depreciation_method")));
                schedule.setTotalDepreciation(rs.getBigDecimal("total_depreciation"));
                schedule.setFinalBookValue(rs.getBigDecimal("final_book_value"));
                schedule.setStatus(rs.getString("status"));
                schedule.setCalculationDate(rs.getTimestamp("calculation_date").toLocalDateTime());

                schedules.add(schedule);
            }
        }

        return schedules;
    }

    /**
     * Find depreciation schedule by ID with entries
     */
    public Optional<DepreciationSchedule> findScheduleById(Long scheduleId) throws SQLException {
        String scheduleSql = """
            SELECT * FROM depreciation_schedules WHERE id = ?
            """;

        String entriesSql = """
            SELECT * FROM depreciation_entries
            WHERE depreciation_schedule_id = ?
            ORDER BY year_number
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement scheduleStmt = conn.prepareStatement(scheduleSql);
             PreparedStatement entriesStmt = conn.prepareStatement(entriesSql)) {

            scheduleStmt.setLong(1, scheduleId);
            ResultSet scheduleRs = scheduleStmt.executeQuery();

            if (scheduleRs.next()) {
                DepreciationSchedule schedule = mapScheduleFromResultSet(scheduleRs);

                // Load entries
                entriesStmt.setLong(1, scheduleId);
                ResultSet entriesRs = entriesStmt.executeQuery();

                List<DepreciationYear> years = new ArrayList<>();
                while (entriesRs.next()) {
                    DepreciationYear year = new DepreciationYear(
                        entriesRs.getInt("year_number"),
                        entriesRs.getBigDecimal("depreciation_amount"),
                        entriesRs.getBigDecimal("cumulative_depreciation"),
                        entriesRs.getBigDecimal("book_value")
                    );
                    years.add(year);
                }

                schedule.setYears(years);
                return Optional.of(schedule);
            }
        }

        return Optional.empty();
    }

    /**
     * Save an asset
     */
    public Asset saveAsset(Asset asset, Long companyId) throws SQLException {
        if (asset.getId() != null) {
            return updateAsset(asset, companyId);
        } else {
            return insertAsset(asset, companyId);
        }
    }

    /**
     * Insert a new asset
     */
    private Asset insertAsset(Asset asset, Long companyId) throws SQLException {
        String sql = """
            INSERT INTO assets (
                company_id, asset_code, asset_name, description, asset_category,
                acquisition_date, cost, salvage_value, useful_life_years,
                location, department, status, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(ASSET_COMPANY_ID_PARAM, companyId);
            stmt.setString(ASSET_CODE_PARAM, asset.getAssetCode());
            stmt.setString(ASSET_NAME_PARAM, asset.getAssetName());
            stmt.setString(ASSET_DESCRIPTION_PARAM, asset.getDescription());
            stmt.setString(ASSET_CATEGORY_PARAM, asset.getAssetCategory());
            stmt.setDate(ASSET_ACQUISITION_DATE_PARAM, Date.valueOf(asset.getAcquisitionDate()));
            stmt.setBigDecimal(ASSET_COST_PARAM, asset.getCost());
            stmt.setBigDecimal(ASSET_SALVAGE_VALUE_PARAM, asset.getSalvageValue());
            stmt.setInt(ASSET_USEFUL_LIFE_PARAM, asset.getUsefulLifeYears());
            stmt.setString(ASSET_LOCATION_PARAM, asset.getLocation());
            stmt.setString(ASSET_DEPARTMENT_PARAM, asset.getDepartment());
            stmt.setString(ASSET_STATUS_PARAM, asset.getStatus());
            stmt.setString(ASSET_CREATED_BY_PARAM, asset.getCreatedBy());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                asset.setId(rs.getLong(1));
                return asset;
            }
        }

        throw new SQLException("Failed to save asset");
    }

    /**
     * Update an existing asset
     */
    private Asset updateAsset(Asset asset, Long companyId) throws SQLException {
        // Check if the new asset code would conflict with existing assets for this company
        if (assetCodeExists(asset.getAssetCode(), companyId)) {
            // Check if the existing asset with this code is not the same asset we're updating
            Optional<Asset> existingAsset = findAssetById(asset.getId());
            if (existingAsset.isEmpty() || !existingAsset.get().getAssetCode().equals(asset.getAssetCode())) {
                throw new SQLException("Asset code '" + asset.getAssetCode() + "' already exists for this company");
            }
        }

        String sql = """
            UPDATE assets SET
                asset_code = ?, asset_name = ?, description = ?, asset_category = ?,
                acquisition_date = ?, cost = ?, salvage_value = ?, useful_life_years = ?,
                location = ?, department = ?, status = ?, accumulated_depreciation = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND company_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, asset.getAssetCode());
            stmt.setString(2, asset.getAssetName());
            stmt.setString(3, asset.getDescription());
            stmt.setString(4, asset.getAssetCategory());
            stmt.setDate(5, Date.valueOf(asset.getAcquisitionDate()));
            stmt.setBigDecimal(6, asset.getCost());
            stmt.setBigDecimal(7, asset.getSalvageValue());
            stmt.setInt(8, asset.getUsefulLifeYears());
            stmt.setString(9, asset.getLocation());
            stmt.setString(10, asset.getDepartment());
            stmt.setString(11, asset.getStatus());
            stmt.setBigDecimal(12, asset.getAccumulatedDepreciation());
            stmt.setLong(13, asset.getId());
            stmt.setLong(14, companyId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Asset not found with ID: " + asset.getId());
            }

            return asset;
        }
    }

    /**
     * Find assets by company
     */
    public List<Asset> findAssetsByCompany(Long companyId) throws SQLException {
        String sql = """
            SELECT * FROM assets
            WHERE company_id = ? AND status = 'ACTIVE'
            ORDER BY asset_code
            """;

        List<Asset> assets = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                assets.add(mapAssetFromResultSet(rs));
            }
        }

        return assets;
    }

    /**
     * Generate a unique schedule number
     */
    private String generateScheduleNumber(Long companyId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM depreciation_schedules WHERE company_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1) + 1;
                return String.format("DEP-%d-%04d", companyId, count);
            }
        }

        return String.format("DEP-%d-0001", companyId);
    }

    private DepreciationSchedule mapScheduleFromResultSet(ResultSet rs) throws SQLException {
        DepreciationSchedule schedule = new DepreciationSchedule();
        schedule.setId(rs.getLong("id"));
        schedule.setAssetId(rs.getLong("asset_id"));
        schedule.setScheduleNumber(rs.getString("schedule_number"));
        schedule.setScheduleName(rs.getString("schedule_name"));
        schedule.setCost(rs.getBigDecimal("cost"));
        schedule.setSalvageValue(rs.getBigDecimal("salvage_value"));
        schedule.setUsefulLifeYears(rs.getInt("useful_life_years"));
        schedule.setDepreciationMethod(DepreciationMethod.valueOf(rs.getString("depreciation_method")));
        schedule.setDbFactor(rs.getBigDecimal("db_factor"));
        schedule.setConvention(rs.getString("convention"));
        schedule.setTotalDepreciation(rs.getBigDecimal("total_depreciation"));
        schedule.setFinalBookValue(rs.getBigDecimal("final_book_value"));
        schedule.setStatus(rs.getString("status"));
        schedule.setCalculationDate(rs.getTimestamp("calculation_date").toLocalDateTime());
        schedule.setJournalEntryId(rs.getObject("journal_entry_id", Long.class));
        schedule.setPostedAt(rs.getTimestamp("posted_at") != null ? rs.getTimestamp("posted_at").toLocalDateTime() : null);
        schedule.setPostedBy(rs.getString("posted_by"));
        return schedule;
    }

    /**
     * Map ResultSet to Asset
     */
    private Asset mapAssetFromResultSet(ResultSet rs) throws SQLException {
        Asset asset = new Asset();
        asset.setId(rs.getLong("id"));
        asset.setCompanyId(rs.getLong("company_id"));
        asset.setAssetCode(rs.getString("asset_code"));
        asset.setAssetName(rs.getString("asset_name"));
        asset.setDescription(rs.getString("description"));
        asset.setAssetCategory(rs.getString("asset_category"));
        asset.setAcquisitionDate(rs.getDate("acquisition_date").toLocalDate());
        asset.setCost(rs.getBigDecimal("cost"));
        asset.setSalvageValue(rs.getBigDecimal("salvage_value"));
        asset.setUsefulLifeYears(rs.getInt("useful_life_years"));
        asset.setLocation(rs.getString("location"));
        asset.setDepartment(rs.getString("department"));
        asset.setStatus(rs.getString("status"));
        asset.setAccumulatedDepreciation(rs.getBigDecimal("accumulated_depreciation"));
        asset.setCreatedBy(rs.getString("created_by"));
        return asset;
    }

    /**
     * Find depreciation schedule by asset ID
     */
    public Optional<DepreciationSchedule> findScheduleByAssetId(Long assetId) throws SQLException {
        String scheduleSql = """
            SELECT * FROM depreciation_schedules WHERE asset_id = ?
            """;

        String entriesSql = """
            SELECT * FROM depreciation_entries
            WHERE depreciation_schedule_id = ?
            ORDER BY year_number
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement scheduleStmt = conn.prepareStatement(scheduleSql);
             PreparedStatement entriesStmt = conn.prepareStatement(entriesSql)) {

            scheduleStmt.setLong(1, assetId);
            ResultSet scheduleRs = scheduleStmt.executeQuery();

            if (scheduleRs.next()) {
                DepreciationSchedule schedule = mapScheduleFromResultSet(scheduleRs);

                // Load entries
                entriesStmt.setLong(1, schedule.getId());
                ResultSet entriesRs = entriesStmt.executeQuery();

                List<DepreciationYear> years = new ArrayList<>();
                while (entriesRs.next()) {
                    DepreciationYear year = new DepreciationYear(
                        entriesRs.getInt("year_number"),
                        entriesRs.getBigDecimal("depreciation_amount"),
                        entriesRs.getBigDecimal("cumulative_depreciation"),
                        entriesRs.getBigDecimal("book_value")
                    );
                    years.add(year);
                }

                schedule.setYears(years);
                return Optional.of(schedule);
            }
        }

        return Optional.empty();
    }

    /**
     * Find all depreciation schedules for an asset
     */
    public List<DepreciationSchedule> findSchedulesByAssetId(Long assetId) throws SQLException {
        String scheduleSql = """
            SELECT * FROM depreciation_schedules WHERE asset_id = ?
            ORDER BY calculation_date DESC
            """;

        String entriesSql = """
            SELECT * FROM depreciation_entries
            WHERE depreciation_schedule_id = ?
            ORDER BY year_number
            """;

        List<DepreciationSchedule> schedules = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement scheduleStmt = conn.prepareStatement(scheduleSql)) {

            scheduleStmt.setLong(1, assetId);
            ResultSet scheduleRs = scheduleStmt.executeQuery();

            while (scheduleRs.next()) {
                DepreciationSchedule schedule = mapScheduleFromResultSet(scheduleRs);

                // Load entries for this schedule using a separate statement
                try (PreparedStatement entriesStmt = conn.prepareStatement(entriesSql)) {
                    entriesStmt.setLong(1, schedule.getId());
                    ResultSet entriesRs = entriesStmt.executeQuery();

                    List<DepreciationYear> years = new ArrayList<>();
                    while (entriesRs.next()) {
                        DepreciationYear year = new DepreciationYear(
                            entriesRs.getInt("year_number"),
                            entriesRs.getBigDecimal("depreciation_amount"),
                            entriesRs.getBigDecimal("cumulative_depreciation"),
                            entriesRs.getBigDecimal("book_value")
                        );
                        years.add(year);
                    }

                    schedule.setYears(years);
                }
                schedules.add(schedule);
            }
        }

        return schedules;
    }

    /**
     * Find asset by ID
     */
    public Optional<Asset> findAssetById(Long assetId) throws SQLException {
        String sql = """
            SELECT * FROM assets WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, assetId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapAssetFromResultSet(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find asset by code and company
     */
    public Optional<Asset> findAssetByCode(String assetCode, Long companyId) throws SQLException {
        String sql = """
            SELECT * FROM assets WHERE asset_code = ? AND company_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, assetCode);
            stmt.setLong(2, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapAssetFromResultSet(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Delete an asset by ID
     */
    public void deleteAsset(Long assetId) throws SQLException {
        String sql = "DELETE FROM assets WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, assetId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Asset not found with ID: " + assetId);
            }
        }
    }

    /**
     * Delete a depreciation schedule and all its related entries and adjustments
     */
    public void deleteDepreciationSchedule(Long scheduleId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Delete in correct order due to foreign key constraints
            // 1. Delete adjustments first
            String deleteAdjustmentsSql = "DELETE FROM depreciation_adjustments WHERE depreciation_schedule_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteAdjustmentsSql)) {
                stmt.setLong(1, scheduleId);
                stmt.executeUpdate();
            }

            // 2. Delete entries
            String deleteEntriesSql = "DELETE FROM depreciation_entries WHERE depreciation_schedule_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteEntriesSql)) {
                stmt.setLong(1, scheduleId);
                stmt.executeUpdate();
            }

            // 3. Delete the schedule
            String deleteScheduleSql = "DELETE FROM depreciation_schedules WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteScheduleSql)) {
                stmt.setLong(1, scheduleId);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Delete an asset and all related depreciation schedules, entries and adjustments in a single transaction.
     * This avoids partial deletes across multiple connections which can leave FK references.
     * Also handles journal entries and lines that are specifically created for depreciation.
     */
    public void deleteAssetCascade(Long assetId) throws SQLException {
        String selectSchedulesSql = "SELECT id, journal_entry_id FROM depreciation_schedules WHERE asset_id = ?";
        String selectEntriesSql = "SELECT journal_entry_line_id FROM depreciation_entries WHERE depreciation_schedule_id = ? AND journal_entry_line_id IS NOT NULL";
        String deleteAdjustmentsSql = "DELETE FROM depreciation_adjustments WHERE depreciation_schedule_id = ?";
        String deleteEntriesSql = "DELETE FROM depreciation_entries WHERE depreciation_schedule_id = ?";
        String deleteScheduleSql = "DELETE FROM depreciation_schedules WHERE id = ?";
        String deleteJournalLinesSql = "DELETE FROM journal_entry_lines WHERE id = ?";
        String deleteJournalEntrySql = "DELETE FROM journal_entries WHERE id = ?";
        String deleteAssetSql = "DELETE FROM assets WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try {
                conn.setAutoCommit(false);

                // Find schedules and their journal entries
                List<Long> scheduleIds = new ArrayList<>();
                List<Long> journalEntryIds = new ArrayList<>();
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSchedulesSql)) {
                    selectStmt.setLong(1, assetId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        while (rs.next()) {
                            scheduleIds.add(rs.getLong("id"));
                            long journalEntryId = rs.getLong("journal_entry_id");
                            if (!rs.wasNull()) {
                                journalEntryIds.add(journalEntryId);
                            }
                        }
                    }
                }

                // For each schedule, collect journal entry lines and delete children
                List<Long> journalLineIds = new ArrayList<>();
                for (Long scheduleId : scheduleIds) {
                    // Get journal entry lines for this schedule's entries
                    try (PreparedStatement entriesStmt = conn.prepareStatement(selectEntriesSql)) {
                        entriesStmt.setLong(1, scheduleId);
                        try (ResultSet rs = entriesStmt.executeQuery()) {
                            while (rs.next()) {
                                journalLineIds.add(rs.getLong("journal_entry_line_id"));
                            }
                        }
                    }

                    // Delete adjustments for this schedule
                    try (PreparedStatement delAdj = conn.prepareStatement(deleteAdjustmentsSql)) {
                        delAdj.setLong(1, scheduleId);
                        delAdj.executeUpdate();
                    }

                    // Delete entries for this schedule
                    try (PreparedStatement delEntries = conn.prepareStatement(deleteEntriesSql)) {
                        delEntries.setLong(1, scheduleId);
                        delEntries.executeUpdate();
                    }

                    // Delete the schedule
                    try (PreparedStatement delSched = conn.prepareStatement(deleteScheduleSql)) {
                        delSched.setLong(1, scheduleId);
                        delSched.executeUpdate();
                    }
                }

                // Delete journal entry lines (only those referenced by depreciation entries)
                for (Long lineId : journalLineIds) {
                    try (PreparedStatement delLine = conn.prepareStatement(deleteJournalLinesSql)) {
                        delLine.setLong(1, lineId);
                        delLine.executeUpdate();
                    }
                }

                // Delete journal entries (only those referenced by depreciation schedules)
                for (Long entryId : journalEntryIds) {
                    try (PreparedStatement delEntry = conn.prepareStatement(deleteJournalEntrySql)) {
                        delEntry.setLong(1, entryId);
                        delEntry.executeUpdate();
                    }
                }

                // Delete the asset
                try (PreparedStatement delAsset = conn.prepareStatement(deleteAssetSql)) {
                    delAsset.setLong(1, assetId);
                    int assetDel = delAsset.executeUpdate();
                    if (assetDel == 0) {
                        throw new SQLException("Asset not found with ID: " + assetId);
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rb) {
                    throw e;
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Check if asset code already exists for the company
     */
    public boolean assetCodeExists(String assetCode, Long companyId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM assets WHERE asset_code = ? AND company_id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, assetCode);
            stmt.setLong(2, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    /**
     * Update depreciation schedule with journal entry posting information
     */
    public void updateDepreciationSchedulePosting(Long scheduleId, Long journalEntryId, String postedBy) throws SQLException {
        String sql = """
            UPDATE depreciation_schedules 
            SET journal_entry_id = ?, posted_at = CURRENT_TIMESTAMP, posted_by = ?, status = 'POSTED'
            WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, journalEntryId);
            stmt.setString(2, postedBy);
            stmt.setLong(3, scheduleId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Depreciation schedule not found with ID: " + scheduleId);
            }
        }
    }

    /**
     * Update depreciation entries with journal entry line IDs
     */
    public void updateDepreciationEntriesWithJournalLines(Long scheduleId, Map<Integer, Long> yearToLineIdMap) throws SQLException {
        String sql = """
            UPDATE depreciation_entries 
            SET journal_entry_line_id = ?
            WHERE depreciation_schedule_id = ? AND year_number = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Map.Entry<Integer, Long> entry : yearToLineIdMap.entrySet()) {
                stmt.setLong(1, entry.getValue());
                stmt.setLong(2, scheduleId);
                stmt.setInt(3, entry.getKey());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Check if depreciation schedule is already posted
     */
    public boolean isDepreciationSchedulePosted(Long scheduleId) throws SQLException {
        String sql = "SELECT status FROM depreciation_schedules WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, scheduleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                return "POSTED".equals(status);
            }
        }

        return false;
    }

    /**
     * Generate a unique asset code by appending a number if the original code exists
     */
    public String generateUniqueAssetCode(String baseCode, Long companyId) throws SQLException {
        String candidateCode = baseCode;
        int counter = 1;

        while (assetCodeExists(candidateCode, companyId)) {
            candidateCode = baseCode + "-" + counter;
            counter++;
        }

        return candidateCode;
    }

    /**
     * Save a disposal record
     */
    public Disposal saveDisposal(Disposal disposal, Long companyId) throws SQLException {
        String sql = """
            INSERT INTO asset_disposals (
                asset_id, company_id, disposal_date, disposal_type,
                proceeds_received, tax_value, loss_on_disposal, gain_on_disposal,
                reference, description, journal_entry_id, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, disposal.getAssetId());
            stmt.setLong(2, companyId);
            stmt.setDate(3, Date.valueOf(disposal.getDisposalDate()));
            stmt.setString(4, disposal.getDisposalType());
            stmt.setBigDecimal(5, disposal.getProceedsReceived());
            stmt.setBigDecimal(6, disposal.getTaxValue());
            stmt.setBigDecimal(7, disposal.getLossOnDisposal());
            stmt.setBigDecimal(8, disposal.getGainOnDisposal());
            stmt.setString(9, disposal.getReference());
            stmt.setString(10, disposal.getDescription());
            stmt.setObject(11, disposal.getJournalEntryId());
            stmt.setString(12, disposal.getCreatedBy());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                disposal.setId(rs.getLong(1));
                return disposal;
            }
        }

        throw new SQLException("Failed to save disposal record");
    }

    /**
     * Update disposal with journal entry ID
     */
    public void updateDisposalJournalEntry(Long disposalId, Long journalEntryId) throws SQLException {
        String sql = """
            UPDATE asset_disposals
            SET journal_entry_id = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, journalEntryId);
            stmt.setLong(2, disposalId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Disposal record not found with ID: " + disposalId);
            }
        }
    }

    /**
     * Update asset status
     */
    public void updateAssetStatus(Long assetId, String status) throws SQLException {
        String sql = """
            UPDATE assets
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setLong(2, assetId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Asset not found with ID: " + assetId);
            }
        }
    }

    /**
     * Find disposal by ID
     */
    public Optional<Disposal> findDisposalById(Long disposalId) throws SQLException {
        String sql = """
            SELECT * FROM asset_disposals WHERE id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, disposalId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapDisposalFromResultSet(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Find disposals by company
     */
    public List<Disposal> findDisposalsByCompany(Long companyId) throws SQLException {
        String sql = """
            SELECT * FROM asset_disposals
            WHERE company_id = ?
            ORDER BY disposal_date DESC
            """;

        List<Disposal> disposals = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                disposals.add(mapDisposalFromResultSet(rs));
            }
        }

        return disposals;
    }

    /**
     * Find disposals by asset
     */
    public List<Disposal> findDisposalsByAsset(Long assetId) throws SQLException {
        String sql = """
            SELECT * FROM asset_disposals
            WHERE asset_id = ?
            ORDER BY disposal_date DESC
            """;

        List<Disposal> disposals = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, assetId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                disposals.add(mapDisposalFromResultSet(rs));
            }
        }

        return disposals;
    }

    /**
     * Find depreciation schedules by status for a company
     */
    public List<DepreciationSchedule> findDepreciationSchedulesByStatus(Long companyId, String status) throws SQLException {
        String scheduleSql = """
            SELECT * FROM depreciation_schedules 
            WHERE company_id = ? AND status = ?
            ORDER BY calculation_date DESC
            """;

        String entriesSql = """
            SELECT * FROM depreciation_entries
            WHERE depreciation_schedule_id = ?
            ORDER BY year_number
            """;

        List<DepreciationSchedule> schedules = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement scheduleStmt = conn.prepareStatement(scheduleSql)) {

            scheduleStmt.setLong(1, companyId);
            scheduleStmt.setString(2, status);
            ResultSet scheduleRs = scheduleStmt.executeQuery();

            while (scheduleRs.next()) {
                DepreciationSchedule schedule = mapScheduleFromResultSet(scheduleRs);

                // Load entries for this schedule using a separate statement
                try (PreparedStatement entriesStmt = conn.prepareStatement(entriesSql)) {
                    entriesStmt.setLong(1, schedule.getId());
                    ResultSet entriesRs = entriesStmt.executeQuery();

                    List<DepreciationYear> years = new ArrayList<>();
                    while (entriesRs.next()) {
                        DepreciationYear year = new DepreciationYear(
                            entriesRs.getInt("year_number"),
                            entriesRs.getBigDecimal("depreciation_amount"),
                            entriesRs.getBigDecimal("cumulative_depreciation"),
                            entriesRs.getBigDecimal("book_value")
                        );
                        years.add(year);
                    }

                    schedule.setYears(years);
                }
                schedules.add(schedule);
            }
        }

        return schedules;
    }

    /**
     * Map ResultSet to Disposal
     */
    private Disposal mapDisposalFromResultSet(ResultSet rs) throws SQLException {
        Disposal disposal = new Disposal();
        disposal.setId(rs.getLong("id"));
        disposal.setAssetId(rs.getLong("asset_id"));
        disposal.setCompanyId(rs.getLong("company_id"));
        disposal.setDisposalDate(rs.getDate("disposal_date").toLocalDate());
        disposal.setDisposalType(rs.getString("disposal_type"));
        disposal.setProceedsReceived(rs.getBigDecimal("proceeds_received"));
        disposal.setTaxValue(rs.getBigDecimal("tax_value"));
        disposal.setLossOnDisposal(rs.getBigDecimal("loss_on_disposal"));
        disposal.setGainOnDisposal(rs.getBigDecimal("gain_on_disposal"));
        disposal.setReference(rs.getString("reference"));
        disposal.setDescription(rs.getString("description"));
        disposal.setJournalEntryId(rs.getObject("journal_entry_id", Long.class));
        disposal.setCreatedBy(rs.getString("created_by"));
        disposal.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        disposal.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return disposal;
    }

    /**
     * Get the fiscal year end date for a given year (February 28/29)
     */
    private java.time.LocalDate getFiscalYearEnd(int year) {
        // February 28 or 29 depending on leap year
        return java.time.LocalDate.of(year, 2, java.time.Year.of(year).isLeap() ? 29 : 28);
    }

    /**
     * Get the fiscal year start date for a given year number relative to acquisition
     */
    private java.time.LocalDate getFiscalYearStart(java.time.LocalDate acquisitionDate, int yearNumber) {
        if (yearNumber == 0) {
            // Year 0 starts on acquisition date
            return acquisitionDate;
        } else {
            // Years 1+ start on March 1 of the year after acquisition + yearNumber
            int targetYear = acquisitionDate.getYear() + yearNumber;
            return java.time.LocalDate.of(targetYear, 3, 1);
        }
    }

    /**
     * Get the fiscal year end date for a given year number relative to acquisition
     */
    private java.time.LocalDate getFiscalYearEnd(java.time.LocalDate acquisitionDate, int yearNumber) {
        // For all years, fiscal year ends on Feb 28/29 of the year after acquisition + yearNumber + 1
        int targetYear = acquisitionDate.getYear() + yearNumber + 1;
        return getFiscalYearEnd(targetYear);
    }

    /**
     * Calculate the fiscal year for a given year number relative to acquisition
     */
    private int calculateFiscalYear(java.time.LocalDate acquisitionDate, int yearNumber) {
        return acquisitionDate.getYear() + yearNumber;
    }
}
