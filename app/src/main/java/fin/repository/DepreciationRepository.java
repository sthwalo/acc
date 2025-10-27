package fin.repository;

import fin.model.Asset;
import fin.model.DepreciationMethod;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
                status, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long scheduleId = rs.getLong(1);
                schedule.setId(scheduleId);

                // Save the depreciation entries
                saveDepreciationEntries(scheduleId, schedule.getYears(), conn);

                return schedule;
            }
        }

        throw new SQLException("Failed to save depreciation schedule");
    }

    /**
     * Save depreciation entries for a schedule
     */
    private void saveDepreciationEntries(Long scheduleId, List<DepreciationYear> years, Connection conn) throws SQLException {
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
                stmt.setInt(ENTRY_FISCAL_YEAR_PARAM, LocalDate.now().getYear() + year.getYear() - 1);
                stmt.setDate(ENTRY_PERIOD_START_PARAM, Date.valueOf(LocalDate.now().withDayOfYear(1).plusYears(year.getYear() - 1)));
                stmt.setDate(ENTRY_PERIOD_END_PARAM, Date.valueOf(LocalDate.now().withDayOfYear(1).plusYears(year.getYear()).minusDays(1)));
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
        return schedule;
    }

    private Asset mapAssetFromResultSet(ResultSet rs) throws SQLException {
        Asset asset = new Asset();
        asset.setId(rs.getLong("id"));
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
        asset.setCurrentBookValue(rs.getBigDecimal("current_book_value"));
        return asset;
    }
}
