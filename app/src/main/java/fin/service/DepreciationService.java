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

package fin.service;

import fin.model.Asset;
import fin.model.DepreciationMethod;
import fin.model.DepreciationRequest;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;
import fin.model.Disposal;
import fin.repository.AccountRepository;
import fin.repository.DepreciationRepository;
import fin.repository.FiscalPeriodRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for calculating depreciation using various methods
 */
public class DepreciationService {
    private final DepreciationRepository depreciationRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final String dbUrl;

    /**
     * Result class for journal entry creation
     */
    private static class JournalEntryResult {
        private final long depreciationExpenseLineId;
        
        public JournalEntryResult(long depreciationExpenseLineId) {
            this.depreciationExpenseLineId = depreciationExpenseLineId;
        }
        
        public long getDepreciationExpenseLineId() { return depreciationExpenseLineId; }
    }

    public DepreciationService(DepreciationRepository repository) {
        this.depreciationRepository = repository;
        this.dbUrl = null; // For backward compatibility
        this.accountRepository = null;
        this.fiscalPeriodRepository = null;
    }

    public DepreciationService(String dbUrl, DepreciationRepository repository) {
        this.depreciationRepository = repository;
        this.dbUrl = dbUrl;
        this.accountRepository = new AccountRepository(dbUrl);
        this.fiscalPeriodRepository = new FiscalPeriodRepository(dbUrl);
    }

    private static final Map<Integer, double[]> FIN_RATES = Map.of(
        5, new double[]{0.20, 0.32, 0.192, 0.1152, 0.1152, 0.0576},
        7, new double[]{0.1429, 0.2449, 0.1749, 0.1249, 0.0893, 0.0892, 0.0893, 0.0446}
    );

    private static final BigDecimal DEFAULT_DB_FACTOR = BigDecimal.valueOf(2.0);
    /**
     * Calculate depreciation schedule for the given request and save to database
     */
    public DepreciationSchedule calculateDepreciation(DepreciationRequest request) {
        validateRequest(request);

        List<DepreciationYear> schedule = calculateDepreciationSchedule(request, request.getAcquisitionDate());

        DepreciationSchedule depreciationSchedule = new DepreciationSchedule(schedule);
        return depreciationSchedule;
    }    /**
     * Calculate depreciation schedule for the given asset and request, then save to database
     */
    public DepreciationSchedule calculateAndSaveDepreciation(Asset asset, DepreciationRequest request) {
        validateRequest(request);

        // Calculate the depreciation schedule
        List<DepreciationYear> schedule = calculateDepreciationSchedule(request, asset.getAcquisitionDate());

        // Create database schedule object
        DepreciationSchedule dbSchedule = new DepreciationSchedule(schedule);
        dbSchedule.setAssetId(asset.getId());
        dbSchedule.setScheduleName(asset.getAssetName() + " - " + request.getMethod().name());
        dbSchedule.setDescription("Depreciation schedule for " + asset.getAssetName());
        dbSchedule.setCost(request.getCost());
        dbSchedule.setSalvageValue(request.getSalvageValue());
        dbSchedule.setUsefulLifeYears(request.getUsefulLife());
        dbSchedule.setDepreciationMethod(request.getMethod());
        dbSchedule.setDbFactor(request.getDbFactor());
        dbSchedule.setConvention(request.getConvention() != null ? request.getConvention() : "HALF_YEAR");
        dbSchedule.setStatus("CALCULATED");
        dbSchedule.setCreatedBy("FIN");

        try {
            // Save to database
            DepreciationSchedule savedSchedule = depreciationRepository.saveDepreciationSchedule(dbSchedule, asset.getCompanyId());

            // Post depreciation to journal entries if not already posted
            if (accountRepository != null && !"POSTED".equals(savedSchedule.getStatus())) {
                postDepreciationToJournal(savedSchedule, asset.getCompanyId());
            }

            return savedSchedule;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save depreciation schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Find depreciation schedule by ID
     */
    public Optional<DepreciationSchedule> getDepreciationSchedule(Long scheduleId) {
        try {
            return depreciationRepository.findScheduleById(scheduleId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find depreciation schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Find depreciation schedule for asset
     */
    public Optional<DepreciationSchedule> getDepreciationScheduleForAsset(Long assetId) {
        try {
            return depreciationRepository.findScheduleByAssetId(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find depreciation schedule for asset: " + e.getMessage(), e);
        }
    }

    /**
     * Get all depreciation schedules for an asset
     */
    public List<DepreciationSchedule> getDepreciationSchedulesForAsset(Long assetId) {
        try {
            List<DepreciationSchedule> schedules = depreciationRepository.findSchedulesByAssetId(assetId);
            return schedules;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find depreciation schedules for asset: " + e.getMessage(), e);
        }
    }

    /**
     * Find asset by ID
     */
    public Optional<Asset> getAssetById(Long assetId) {
        try {
            return depreciationRepository.findAssetById(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find asset: " + e.getMessage(), e);
        }
    }

    /**
     * Get assets for company
     */
    public List<Asset> getAssetsForCompany(Long companyId) {
        try {
            return depreciationRepository.findAssetsByCompany(companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find assets for company: " + e.getMessage(), e);
        }
    }

    /**
     * Find asset by code and company
     */
    public Optional<Asset> getAssetByCode(String assetCode, Long companyId) {
        try {
            return depreciationRepository.findAssetByCode(assetCode, companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find asset by code: " + e.getMessage(), e);
        }
    }

    /**
     * Save asset
     */
    public Asset saveAsset(Asset asset) {
        try {
            return depreciationRepository.saveAsset(asset, asset.getCompanyId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save asset", e);
        }
    }

    /**
     * Delete depreciation schedule
     */
    public void deleteDepreciationSchedule(Long scheduleId) {
        try {
            depreciationRepository.deleteDepreciationSchedule(scheduleId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete depreciation schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Delete asset
     */
    public void deleteAsset(Long assetId) {
        try {
            // Use repository-level cascade delete in a single transaction
            depreciationRepository.deleteAssetCascade(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete asset: " + e.getMessage(), e);
        }
    }

    /**
     * Check if asset code already exists for the company
     */
    public boolean assetCodeExists(String assetCode, Long companyId) {
        try {
            return depreciationRepository.assetCodeExists(assetCode, companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check asset code existence: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a unique asset code by appending a number if the original code exists
     */
    public String generateUniqueAssetCode(String baseCode, Long companyId) {
        try {
            return depreciationRepository.generateUniqueAssetCode(baseCode, companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate unique asset code: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate straight-line depreciation
     */
    public BigDecimal calculateStraightLineDepreciation(BigDecimal cost, BigDecimal salvageValue, int usefulLife) {
        if (usefulLife <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        return cost.subtract(salvageValue).divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate declining balance depreciation for a specific year
     */
    public BigDecimal calculateDecliningBalanceDepreciation(BigDecimal bookValue, BigDecimal rate, int usefulLife) {
        return bookValue.multiply(rate).divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate FIN depreciation for a specific year
     */
    public BigDecimal calculateFINDepreciation(BigDecimal basis, int recoveryPeriod, int year) {
        double[] rates = FIN_RATES.get(recoveryPeriod);
        if (rates == null) {
            throw new IllegalArgumentException("Unsupported recovery period: " + recoveryPeriod +
                                             ". Supported periods: " + FIN_RATES.keySet());
        }
        if (year < 0 || year > rates.length) {  // Changed from year < 1 to year < 0
            return BigDecimal.ZERO;
        }
        if (year == 0) {
            return BigDecimal.ZERO;  // Year 0 has no depreciation for FIN method
        }
        return basis.multiply(BigDecimal.valueOf(rates[year - 1]));
    }

    /**
     * Get supported FIN recovery periods
     */
    public Set<Integer> getSupportedFINPeriods() {
        return FIN_RATES.keySet();
    }

    /**
     * Get FIN rates for a specific recovery period
     */
    public double[] getFINRates(int recoveryPeriod) {
        double[] rates = FIN_RATES.get(recoveryPeriod);
        if (rates == null) {
            throw new IllegalArgumentException("Unsupported recovery period: " + recoveryPeriod);
        }
        return Arrays.copyOf(rates, rates.length);
    }

    /**
     * Calculate depreciation schedule for the given request
     */
    private List<DepreciationYear> calculateDepreciationSchedule(DepreciationRequest request, java.time.LocalDate acquisitionDate) {
        List<DepreciationYear> schedule = new ArrayList<>();
        BigDecimal bookValue = request.getCost();
        BigDecimal cumulativeDepreciation = BigDecimal.ZERO;

        for (int year = 0; year < request.getUsefulLife(); year++) {
            BigDecimal annualDepreciation = calculateAnnualDepreciation(
                request.getMethod(),
                bookValue,
                request.getCost(),
                request.getSalvageValue(),
                request.getUsefulLife(),
                year,
                request.getDbFactor()
            );

            // For Year 0: calculate partial year depreciation based on months from acquisition to fiscal year-end
            if (year == 0 && acquisitionDate != null) {
                BigDecimal partialYearFactor = calculatePartialYearFactor(acquisitionDate);
                annualDepreciation = annualDepreciation.multiply(partialYearFactor);
                annualDepreciation = annualDepreciation.setScale(2, RoundingMode.HALF_UP);
            }

            // Ensure we don't depreciate below salvage value for SL method
            if (request.getMethod() == DepreciationMethod.STRAIGHT_LINE) {
                BigDecimal remainingValue = request.getCost().subtract(cumulativeDepreciation);
                BigDecimal minValue = request.getSalvageValue();
                if (remainingValue.subtract(annualDepreciation).compareTo(minValue) < 0) {
                    annualDepreciation = remainingValue.subtract(minValue);
                }
            }

            annualDepreciation = annualDepreciation.setScale(2, RoundingMode.HALF_UP);

            cumulativeDepreciation = cumulativeDepreciation.add(annualDepreciation);
            bookValue = request.getCost().subtract(cumulativeDepreciation);

            DepreciationYear yearEntry = new DepreciationYear(
                year,
                annualDepreciation,
                cumulativeDepreciation,
                bookValue
            );
            schedule.add(yearEntry);
        }

        return schedule;
    }

    private BigDecimal calculateAnnualDepreciation(
            DepreciationMethod method,
            BigDecimal bookValue,
            BigDecimal cost,
            BigDecimal salvageValue,
            int usefulLife,
            int currentYear,
            BigDecimal dbFactor) {

        switch (method) {
            case STRAIGHT_LINE:
                return calculateStraightLineDepreciation(cost, salvageValue, usefulLife);

            case DECLINING_BALANCE:
                if (dbFactor == null) {
                    dbFactor = DEFAULT_DB_FACTOR; // Default to double declining balance
                }
                // dbFactor is the total rate, divide by usefulLife to get annual rate
                BigDecimal annualRate = dbFactor.divide(BigDecimal.valueOf(usefulLife), 6, RoundingMode.HALF_UP);
                return bookValue.multiply(annualRate);

            case FIN:
                return calculateFINDepreciation(cost, usefulLife, currentYear);

            default:
                throw new IllegalArgumentException("Unsupported depreciation method: " + method);
        }
    }

    /**
     * Post depreciation schedule to journal entries - simplified to post only current year's annual depreciation
     */
    private void postDepreciationToJournal(DepreciationSchedule schedule, Long companyId) throws SQLException {
        // Get account IDs for Depreciation Expense (9400) and Accumulated Depreciation (2100)
        Long depreciationExpenseAccountId = accountRepository.getAccountIdByCode(companyId, "9400");
        Long accumulatedDepreciationAccountId = accountRepository.getAccountIdByCode(companyId, "2100");

        if (depreciationExpenseAccountId == null) {
            throw new SQLException("Depreciation Expense account (9400) not found for company " + companyId);
        }
        if (accumulatedDepreciationAccountId == null) {
            throw new SQLException("Accumulated Depreciation account (2100) not found for company " + companyId);
        }

        // Get asset acquisition date
        Asset asset = getAssetById(schedule.getAssetId()).orElseThrow(() ->
            new SQLException("Asset not found for schedule: " + schedule.getId()));
        java.time.LocalDate acquisitionDate = asset.getAcquisitionDate();

        // Calculate current fiscal year relative to acquisition date
        int currentDepreciationYear = calculateCurrentDepreciationYear(acquisitionDate);

        // Check if we're within the asset's useful life
        if (currentDepreciationYear >= schedule.getYears().size()) {
            // Asset fully depreciated, no more depreciation to post
            return;
        }

        // Get the depreciation for the current fiscal year
        DepreciationYear currentYearDepreciation = schedule.getYears().get(currentDepreciationYear);

        // Check if this year's depreciation has already been posted
        if (isDepreciationYearPosted(schedule.getId(), currentDepreciationYear)) {
            // Update asset accumulated depreciation to cumulative amount up to this year
            updateAssetAccumulatedDepreciation(schedule, asset, currentDepreciationYear);
            return;
        }

        // Calculate the depreciation recognition date (fiscal year end)
        java.time.LocalDate depreciationDate = getFiscalYearEnd(acquisitionDate.getYear() + currentDepreciationYear + 1);

        // Create journal entry for this year's depreciation
        JournalEntryResult result = createJournalEntryForDepreciationYear(schedule, currentYearDepreciation,
            companyId, depreciationExpenseAccountId, accumulatedDepreciationAccountId, depreciationDate);

        // Update depreciation entry with journal entry line ID
        updateDepreciationEntryWithJournalLine(schedule.getId(), currentDepreciationYear, result.getDepreciationExpenseLineId());

        // Update asset accumulated depreciation to cumulative amount up to this year
        updateAssetAccumulatedDepreciation(schedule, asset, currentDepreciationYear);
    }

    /**
     * Get the current (active) fiscal period for a company based on the current date
     */
    private Long getCurrentFiscalPeriodId(Long companyId) throws SQLException {
        if (fiscalPeriodRepository == null) {
            throw new SQLException("FiscalPeriodRepository not available");
        }

        // Get current date
        java.time.LocalDate currentDate = java.time.LocalDate.now();

        // Find all fiscal periods for the company
        List<fin.model.FiscalPeriod> allPeriods = fiscalPeriodRepository.findByCompanyId(companyId);
        if (allPeriods.isEmpty()) {
            throw new SQLException("No fiscal periods found for company " + companyId);
        }

        // Find the fiscal period that contains the current date
        for (fin.model.FiscalPeriod period : allPeriods) {
            if (!period.isClosed() &&
                (currentDate.isEqual(period.getStartDate()) || currentDate.isAfter(period.getStartDate())) &&
                (currentDate.isEqual(period.getEndDate()) || currentDate.isBefore(period.getEndDate()))) {
                return period.getId();
            }
        }

        // If no current period found, return the most recent open period
        fin.model.FiscalPeriod mostRecent = null;
        for (fin.model.FiscalPeriod period : allPeriods) {
            if (!period.isClosed()) {
                if (mostRecent == null || period.getEndDate().isAfter(mostRecent.getEndDate())) {
                    mostRecent = period;
                }
            }
        }

        if (mostRecent != null) {
            return mostRecent.getId();
        }

        throw new SQLException("No active fiscal period found for company " + companyId + " on date " + currentDate);
    }

    /**
     * Get the fiscal period ID for a specific date
     */
    private Long getFiscalPeriodIdForDate(Long companyId, java.time.LocalDate targetDate) throws SQLException {
        if (fiscalPeriodRepository == null) {
            throw new SQLException("FiscalPeriodRepository not available");
        }

        // Find all fiscal periods for the company
        List<fin.model.FiscalPeriod> allPeriods = fiscalPeriodRepository.findByCompanyId(companyId);
        if (allPeriods.isEmpty()) {
            throw new SQLException("No fiscal periods found for company " + companyId);
        }

        // Find the fiscal period that contains the target date
        for (fin.model.FiscalPeriod period : allPeriods) {
            if (!period.isClosed() &&
                (targetDate.isEqual(period.getStartDate()) || targetDate.isAfter(period.getStartDate())) &&
                (targetDate.isEqual(period.getEndDate()) || targetDate.isBefore(period.getEndDate()))) {
                return period.getId();
            }
        }

        // If no period found for the target date, find the most recent open period
        fin.model.FiscalPeriod mostRecent = null;
        for (fin.model.FiscalPeriod period : allPeriods) {
            if (!period.isClosed()) {
                if (mostRecent == null || period.getEndDate().isAfter(mostRecent.getEndDate())) {
                    mostRecent = period;
                }
            }
        }

        if (mostRecent != null) {
            return mostRecent.getId();
        }

        throw new SQLException("No active fiscal period found for company " + companyId + " on date " + targetDate);
    }

    /**
     * Create a journal entry for a specific depreciation year
     */
    private JournalEntryResult createJournalEntryForDepreciationYear(DepreciationSchedule schedule, DepreciationYear year, 
            Long companyId, Long depreciationExpenseAccountId, Long accumulatedDepreciationAccountId, 
            java.time.LocalDate depreciationDate) throws SQLException {
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            // Create journal entry header
            String reference = "DEPR-" + schedule.getId() + "-" + year.getYear();
            String description = "Depreciation expense for " + schedule.getScheduleName() + " - Year " + year.getYear();
            Long fiscalPeriodId = getFiscalPeriodIdForDate(companyId, depreciationDate);
            
            // Insert journal entry
            String insertEntrySql = """
                INSERT INTO journal_entries (reference, entry_date, description, company_id, fiscal_period_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
            
            long journalEntryId;
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(insertEntrySql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, reference);
                stmt.setDate(2, java.sql.Date.valueOf(depreciationDate));
                stmt.setString(3, description);
                stmt.setLong(4, companyId);
                stmt.setLong(5, fiscalPeriodId);
                stmt.setString(6, "FIN");
                
                stmt.executeUpdate();
                
                try (java.sql.ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        journalEntryId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to get journal entry ID");
                    }
                }
            }
            
            // Insert journal entry lines
            String insertLineSql = """
                INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, reference, source_transaction_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            long depreciationExpenseLineId;
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(insertLineSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                // Debit Depreciation Expense
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, depreciationExpenseAccountId);
                stmt.setBigDecimal(3, year.getDepreciation());
                stmt.setNull(4, java.sql.Types.NUMERIC);
                stmt.setString(5, "Depreciation expense");
                stmt.setString(6, "DEPR-" + schedule.getId() + "-" + year.getYear());
                stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for depreciation
                stmt.executeUpdate();
                
                // Get the ID of the depreciation expense line
                try (java.sql.ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        depreciationExpenseLineId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to get depreciation expense journal entry line ID");
                    }
                }
                
                // Credit Accumulated Depreciation
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, accumulatedDepreciationAccountId);
                stmt.setNull(3, java.sql.Types.NUMERIC);
                stmt.setBigDecimal(4, year.getDepreciation());
                stmt.setString(5, "Accumulated depreciation");
                stmt.setString(6, "DEPR-" + schedule.getId() + "-" + year.getYear());
                stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for depreciation
                stmt.executeUpdate();
            }
            
            conn.commit();
            return new JournalEntryResult(depreciationExpenseLineId);
            
        } catch (SQLException e) {
            throw new SQLException("Failed to create journal entry for depreciation year " + year.getYear() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Repost depreciation schedules that have been calculated but not posted
     */
    public void repostDepreciationSchedules(Long companyId) throws SQLException {
        List<DepreciationSchedule> calculatedSchedules = depreciationRepository.findDepreciationSchedulesByStatus(companyId, "CALCULATED");
        
        for (DepreciationSchedule schedule : calculatedSchedules) {
            try {
                System.out.println("Reposting depreciation schedule: " + schedule.getScheduleName());
                postDepreciationToJournal(schedule, companyId);
                System.out.println("Successfully reposted schedule ID: " + schedule.getId());
            } catch (Exception e) {
                System.err.println("Failed to repost schedule ID " + schedule.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Update asset acquisition journal entries to show net book value (cost - accumulated depreciation)
     * This modifies existing asset acquisition entries to reflect the current net book value instead of original cost
     */
    public void updateAssetAcquisitionEntriesToNetBookValue(Long companyId) throws SQLException {
        // Find all assets with accumulated depreciation > 0
        List<Asset> assetsWithDepreciation = depreciationRepository.findAssetsByCompany(companyId).stream()
            .filter(asset -> asset.getAccumulatedDepreciation() != null && 
                           asset.getAccumulatedDepreciation().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        for (Asset asset : assetsWithDepreciation) {
            try {
                updateAssetAcquisitionEntryToNetBookValue(asset);
                System.out.println("Updated acquisition entry for asset: " + asset.getAssetName());
            } catch (Exception e) {
                System.err.println("Failed to update acquisition entry for asset " + asset.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Update a specific asset's acquisition journal entry to show net book value
     */
    private void updateAssetAcquisitionEntryToNetBookValue(Asset asset) throws SQLException {
        // Calculate net book value
        BigDecimal netBookValue = asset.getCost().subtract(asset.getAccumulatedDepreciation());

        // Find the asset acquisition journal entry (look for entries with reference like 'ASSET-%')
        String findAcquisitionEntrySql = """
            SELECT je.id, jel.id as line_id, jel.debit_amount, jel.account_id
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            WHERE je.company_id = ? AND je.reference LIKE 'ASSET-%' 
            AND jel.debit_amount IS NOT NULL AND jel.debit_amount > 0
            AND je.description LIKE '%Acquisition of ' || ? || '%'
            ORDER BY je.entry_date DESC
            LIMIT 1
            """;

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            Long journalEntryLineId = null;
            BigDecimal currentDebitAmount = null;
            Long accountId = null;

            // Find the acquisition entry
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(findAcquisitionEntrySql)) {
                stmt.setLong(1, asset.getCompanyId());
                stmt.setString(2, asset.getAssetName());

                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        journalEntryLineId = rs.getLong("line_id");
                        currentDebitAmount = rs.getBigDecimal("debit_amount");
                        accountId = rs.getLong("account_id");
                    }
                }
            }

            if (journalEntryLineId == null) {
                System.out.println("No acquisition entry found for asset: " + asset.getAssetName());
                return;
            }

            // Only update if the current amount is different from net book value
            if (currentDebitAmount != null && currentDebitAmount.compareTo(netBookValue) == 0) {
                System.out.println("Acquisition entry for asset " + asset.getAssetName() + " already shows net book value");
                return;
            }

            // Update the journal entry line to show net book value
            String updateLineSql = """
                UPDATE journal_entry_lines 
                SET debit_amount = ?, description = ?
                WHERE id = ?
                """;

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(updateLineSql)) {
                stmt.setBigDecimal(1, netBookValue);
                stmt.setString(2, "Asset acquisition - Net book value (cost - accumulated depreciation)");
                stmt.setLong(3, journalEntryLineId);
                stmt.executeUpdate();
            }

            // Update the corresponding credit entry (bank/cash) to match
            String updateCreditSql = """
                UPDATE journal_entry_lines 
                SET credit_amount = ?
                WHERE journal_entry_id = (SELECT journal_entry_id FROM journal_entry_lines WHERE id = ?)
                AND account_id != ?
                AND credit_amount IS NOT NULL
                """;

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(updateCreditSql)) {
                stmt.setBigDecimal(1, netBookValue);
                stmt.setLong(2, journalEntryLineId);
                if (accountId != null) {
                    stmt.setLong(3, accountId);
                } else {
                    // If accountId is null, use a dummy value that won't match any account
                    stmt.setLong(3, -1L);
                }
                stmt.executeUpdate();
            }

            // Update the journal entry description to reflect net book value
            String updateEntrySql = """
                UPDATE journal_entries 
                SET description = ?
                WHERE id = (SELECT journal_entry_id FROM journal_entry_lines WHERE id = ?)
                """;

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(updateEntrySql)) {
                stmt.setString(1, "Acquisition of " + asset.getAssetName() + " asset - Net book value");
                stmt.setLong(2, journalEntryLineId);
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Updated acquisition entry for " + asset.getAssetName() + 
                             " from " + currentDebitAmount + " to net book value " + netBookValue);

        } catch (SQLException e) {
            throw new SQLException("Failed to update asset acquisition entry to net book value: " + e.getMessage(), e);
        }
    }

    /**
     * Dispose of an asset and create appropriate journal entries
     */
    public Disposal disposeAsset(Long assetId, java.time.LocalDate disposalDate, String disposalType,
                                BigDecimal proceedsReceived, String description) throws SQLException {
        try {
            // Get the asset
            Optional<Asset> assetOpt = getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                throw new IllegalArgumentException("Asset not found: " + assetId);
            }
            Asset asset = assetOpt.get();

            // Validate asset can be disposed
            if ("DISPOSED".equals(asset.getStatus())) {
                throw new IllegalArgumentException("Asset is already disposed: " + assetId);
            }

            // Calculate tax value (cost - accumulated depreciation)
            BigDecimal taxValue = calculateTaxValue(asset);

            // Create disposal record
            Disposal disposal = new Disposal();
            disposal.setAssetId(assetId);
            disposal.setCompanyId(asset.getCompanyId());
            disposal.setDisposalDate(disposalDate);
            disposal.setDisposalType(disposalType);
            disposal.setProceedsReceived(proceedsReceived != null ? proceedsReceived : BigDecimal.ZERO);
            disposal.setTaxValue(taxValue);
            disposal.setReference("DISP-" + assetId + "-" + disposalDate.toString().replace("-", ""));
            disposal.setDescription(description != null ? description :
                "Disposal of asset " + asset.getAssetName() + " (" + asset.getAssetCode() + ")");
            disposal.setCreatedBy("FIN");

            // Save disposal record
            Disposal savedDisposal = depreciationRepository.saveDisposal(disposal, asset.getCompanyId());

            // Create journal entries for disposal
            if (accountRepository != null) {
                long journalEntryId = createJournalEntryForDisposal(savedDisposal, asset);
                savedDisposal.setJournalEntryId(journalEntryId);

                // Update disposal with journal entry ID
                depreciationRepository.updateDisposalJournalEntry(savedDisposal.getId(), journalEntryId);
            }

            // Update asset status to DISPOSED
            depreciationRepository.updateAssetStatus(assetId, "DISPOSED");

            return savedDisposal;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to dispose asset: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate the tax value of an asset (cost - accumulated depreciation)
     */
    private BigDecimal calculateTaxValue(Asset asset) {
        BigDecimal cost = asset.getCost() != null ? asset.getCost() : BigDecimal.ZERO;
        BigDecimal accumulatedDep = asset.getAccumulatedDepreciation() != null ?
            asset.getAccumulatedDepreciation() : BigDecimal.ZERO;
        return cost.subtract(accumulatedDep);
    }

    /**
     * Create journal entries for asset disposal per SARS section 11(o) guidelines
     */
    private long createJournalEntryForDisposal(Disposal disposal, Asset asset) throws SQLException {
        // Get required account IDs
        Long fixedAssetAccountId = getFixedAssetAccountId(asset.getCompanyId(), asset.getAssetCategory());
        Long accumulatedDepAccountId = accountRepository.getAccountIdByCode(asset.getCompanyId(), "2100");
        Long cashAccountId = getCashAccountId(asset.getCompanyId());
        Long lossOnDisposalAccountId = accountRepository.getAccountIdByCode(asset.getCompanyId(), "9500");
        Long gainOnDisposalAccountId = accountRepository.getAccountIdByCode(asset.getCompanyId(), "9600");

        if (fixedAssetAccountId == null) {
            throw new SQLException("Fixed Asset account not found for category: " + asset.getAssetCategory());
        }
        if (accumulatedDepAccountId == null) {
            throw new SQLException("Accumulated Depreciation account (2100) not found");
        }

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            // Create journal entry header
            String reference = disposal.getReference();
            java.time.LocalDate entryDate = disposal.getDisposalDate();
            String description = disposal.getDescription();
            Long fiscalPeriodId = getCurrentFiscalPeriodId(asset.getCompanyId());

            // Insert journal entry
            String insertEntrySql = """
                INSERT INTO journal_entries (reference, entry_date, description, company_id, fiscal_period_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            long journalEntryId;
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(insertEntrySql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, reference);
                stmt.setDate(2, java.sql.Date.valueOf(entryDate));
                stmt.setString(3, description);
                stmt.setLong(4, asset.getCompanyId());
                stmt.setLong(5, fiscalPeriodId);
                stmt.setString(6, "FIN");

                stmt.executeUpdate();

                try (java.sql.ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        journalEntryId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to get journal entry ID");
                    }
                }
            }

            // Insert journal entry lines
            String insertLineSql = """
                INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, reference, source_transaction_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(insertLineSql)) {
                // 1. Debit Accumulated Depreciation (remove from balance sheet)
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, accumulatedDepAccountId);
                stmt.setBigDecimal(3, asset.getAccumulatedDepreciation());
                stmt.setNull(4, java.sql.Types.NUMERIC);
                stmt.setString(5, "Accumulated depreciation on disposed asset");
                stmt.setString(6, disposal.getReference() + "-ACCDEP");
                stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for disposal
                stmt.executeUpdate();

                // 2. Credit Fixed Asset (remove original cost from balance sheet)
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, fixedAssetAccountId);
                stmt.setNull(3, java.sql.Types.NUMERIC);
                stmt.setBigDecimal(4, asset.getCost());
                stmt.setString(5, "Fixed asset disposal");
                stmt.setString(6, disposal.getReference() + "-ASSET");
                stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for disposal
                stmt.executeUpdate();

                // 3. Debit Cash/Bank (if proceeds received)
                if (disposal.getProceedsReceived() != null && disposal.getProceedsReceived().compareTo(BigDecimal.ZERO) > 0) {
                    if (cashAccountId == null) {
                        throw new SQLException("Cash/Bank account not found for company " + asset.getCompanyId());
                    }
                    stmt.setLong(1, journalEntryId);
                    stmt.setLong(2, cashAccountId);
                    stmt.setBigDecimal(3, disposal.getProceedsReceived());
                    stmt.setNull(4, java.sql.Types.NUMERIC);
                    stmt.setString(5, "Proceeds from asset disposal");
                    stmt.setString(6, disposal.getReference() + "-PROCEEDS");
                    stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for disposal
                    stmt.executeUpdate();
                }

                // 4. Loss on Disposal (Credit if loss) - Section 11(o) allowance
                if (disposal.hasLoss()) {
                    if (lossOnDisposalAccountId == null) {
                        throw new SQLException("Loss on Disposal account (9500) not found");
                    }
                    stmt.setLong(1, journalEntryId);
                    stmt.setLong(2, lossOnDisposalAccountId);
                    stmt.setNull(3, java.sql.Types.NUMERIC);
                    stmt.setBigDecimal(4, disposal.getLossOnDisposal());
                    stmt.setString(5, "Loss on disposal (Section 11(o) allowance)");
                    stmt.setString(6, disposal.getReference() + "-LOSS");
                    stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for disposal
                    stmt.executeUpdate();
                }

                // 5. Gain on Disposal (Debit if gain) - Recoupment under section 8(4)(a)
                if (disposal.hasGain()) {
                    if (gainOnDisposalAccountId == null) {
                        throw new SQLException("Gain on Disposal account (9600) not found");
                    }
                    stmt.setLong(1, journalEntryId);
                    stmt.setLong(2, gainOnDisposalAccountId);
                    stmt.setBigDecimal(3, disposal.getGainOnDisposal());
                    stmt.setNull(4, java.sql.Types.NUMERIC);
                    stmt.setString(5, "Gain on disposal (Recoupment under section 8(4)(a))");
                    stmt.setString(6, disposal.getReference() + "-GAIN");
                    stmt.setNull(7, java.sql.Types.INTEGER); // source_transaction_id - NULL for disposal
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            return journalEntryId;

        } catch (SQLException e) {
            throw new SQLException("Failed to create journal entry for disposal: " + e.getMessage(), e);
        }
    }

    /**
     * Get fixed asset account ID based on asset category
     */
    private Long getFixedAssetAccountId(Long companyId, String assetCategory) {
        // Map common asset categories to account codes
        String accountCode = switch (assetCategory != null ? assetCategory.toLowerCase() : "") {
            case "machinery", "equipment", "plant" -> "2000"; // Machinery & Equipment
            case "vehicles", "motor vehicles" -> "2100"; // Vehicles (but 2100 is accumulated dep, use 2200)
            case "buildings", "property" -> "2300"; // Buildings
            case "furniture", "fixtures" -> "2400"; // Furniture & Fixtures
            case "computers", "it equipment" -> "2500"; // Computer Equipment
            default -> "2000"; // Default to Machinery & Equipment
        };

        return accountRepository.getAccountIdByCode(companyId, accountCode);
    }

    /**
     * Get cash/bank account ID for the company
     */
    private Long getCashAccountId(Long companyId) {
        // Try common cash account codes
        Long accountId = accountRepository.getAccountIdByCode(companyId, "1100"); // Cash
        if (accountId != null) return accountId;

        accountId = accountRepository.getAccountIdByCode(companyId, "1110"); // Bank
        if (accountId != null) return accountId;

        accountId = accountRepository.getAccountIdByCode(companyId, "1120"); // Petty Cash
        return accountId; // Return whatever we found, or null
    }

    /**
     * Get disposal record by ID
     */
    public Optional<Disposal> getDisposalById(Long disposalId) {
        try {
            return depreciationRepository.findDisposalById(disposalId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find disposal: " + e.getMessage(), e);
        }
    }

    /**
     * Get all disposals for a company
     */
    public List<Disposal> getDisposalsForCompany(Long companyId) {
        try {
            return depreciationRepository.findDisposalsByCompany(companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find disposals for company: " + e.getMessage(), e);
        }
    }

    /**
     * Get disposals for an asset
     */
    public List<Disposal> getDisposalsForAsset(Long assetId) {
        try {
            return depreciationRepository.findDisposalsByAsset(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find disposals for asset: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a depreciation year has already been posted
     */
    private boolean isDepreciationYearPosted(Long scheduleId, int yearNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM depreciation_entries WHERE depreciation_schedule_id = ? AND year_number = ? AND journal_entry_line_id IS NOT NULL";
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleId);
            stmt.setInt(2, yearNumber);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Update asset accumulated depreciation to cumulative amount up to specified year
     */
    private void updateAssetAccumulatedDepreciation(DepreciationSchedule schedule, Asset asset, int upToYear) throws SQLException {
        // Find the cumulative depreciation up to the specified year
        BigDecimal cumulativeDepreciation = BigDecimal.ZERO;
        for (DepreciationYear year : schedule.getYears()) {
            if (year.getYear() <= upToYear) {
                cumulativeDepreciation = year.getCumulativeDepreciation();
            } else {
                break;
            }
        }

        // Update the asset
        asset.setAccumulatedDepreciation(cumulativeDepreciation);
        depreciationRepository.saveAsset(asset, asset.getCompanyId());
    }

    /**
     * Update depreciation entry with journal entry line ID
     */
    private void updateDepreciationEntryWithJournalLine(Long scheduleId, int yearNumber, long journalEntryLineId) throws SQLException {
        String sql = """
            UPDATE depreciation_entries 
            SET journal_entry_line_id = ?, status = 'POSTED', updated_at = CURRENT_TIMESTAMP
            WHERE depreciation_schedule_id = ? AND year_number = ?
            """;
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl);
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journalEntryLineId);
            stmt.setLong(2, scheduleId);
            stmt.setInt(3, yearNumber);
            stmt.executeUpdate();
        }
    }

    /**
     * Calculate partial year factor for Year 0 depreciation (IAS 16 compliance)
     * Returns the fraction of the year from acquisition date to fiscal year-end
     */
    private BigDecimal calculatePartialYearFactor(java.time.LocalDate acquisitionDate) {
        // Find the fiscal year-end for the acquisition year
        // For fiscal years ending in February, if acquisition is after Feb, fiscal year end is next year
        java.time.LocalDate currentFiscalEnd = getFiscalYearEnd(acquisitionDate.getYear());
        java.time.LocalDate fiscalYearEnd;

        if (acquisitionDate.isAfter(currentFiscalEnd)) {
            // Acquisition after Feb, so fiscal year ends next Feb
            fiscalYearEnd = getFiscalYearEnd(acquisitionDate.getYear() + 1);
        } else {
            // Acquisition before or on Feb, fiscal year ends this Feb
            fiscalYearEnd = currentFiscalEnd;
        }

        // Calculate months between acquisition date and fiscal year-end
        java.time.Period period = java.time.Period.between(acquisitionDate, fiscalYearEnd.plusDays(1)); // +1 to include end date

        // Convert to total months (approximate)
        int totalMonths = period.getYears() * 12 + period.getMonths() +
                         (period.getDays() > 15 ? 1 : 0); // Round up if more than 15 days

        // Ensure at least 1 month minimum
        totalMonths = Math.max(1, totalMonths);

        // Return factor (months in partial year / 12)
        return BigDecimal.ONE.divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
    }

    /**
     * Get the fiscal year end date for a given year (February 28/29)
     */
    private java.time.LocalDate getFiscalYearEnd(int year) {
        // February 28 or 29 depending on leap year
        return java.time.LocalDate.of(year, 2, java.time.Year.of(year).isLeap() ? 29 : 28);
    }

    private void validateRequest(DepreciationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Depreciation request cannot be null");
        }
        if (request.getCost() == null || request.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cost must be positive");
        }
        if (request.getSalvageValue() == null || request.getSalvageValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Salvage value cannot be negative");
        }
        if (request.getSalvageValue().compareTo(request.getCost()) >= 0) {
            throw new IllegalArgumentException("Salvage value must be less than cost");
        }
        if (request.getUsefulLife() <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        if (request.getMethod() == null) {
            throw new IllegalArgumentException("Depreciation method cannot be null");
        }
        if (request.getMethod() == DepreciationMethod.FIN && !FIN_RATES.containsKey(request.getUsefulLife())) {
            throw new IllegalArgumentException("FIN method only supports recovery periods: "
                    + FIN_RATES.keySet());
        }
    }

    /**
     * Calculate the current depreciation year (0-based) based on fiscal years since acquisition
     * For IAS 16 compliance with Feb 28/29 fiscal year ends
     */
    private int calculateCurrentDepreciationYear(java.time.LocalDate acquisitionDate) {
        java.time.LocalDate today = java.time.LocalDate.now();

        // Calculate fiscal years passed since acquisition
        int acquisitionYear = acquisitionDate.getYear();
        int currentYear = today.getYear();

        // Count complete fiscal years from acquisition to now
        int depreciationYear = currentYear - acquisitionYear;

        // Adjust based on fiscal year timing
        java.time.LocalDate acquisitionFiscalEnd = getFiscalYearEnd(acquisitionYear);
        java.time.LocalDate currentFiscalEnd = getFiscalYearEnd(currentYear);

        // If acquisition was after fiscal year end, it starts counting from next year
        if (acquisitionDate.isAfter(acquisitionFiscalEnd)) {
            depreciationYear--; // Acquisition starts in next fiscal year
        }

        // If we haven't reached the current fiscal year end yet, we're still in the previous depreciation year
        if (today.isBefore(currentFiscalEnd) || today.isEqual(currentFiscalEnd)) {
            // We're in the current fiscal year, so this is the depreciation year
        } else {
            // We've passed the fiscal year end, so we're starting the next depreciation year
            depreciationYear++;
        }

        return Math.max(0, depreciationYear); // Ensure non-negative
    }
}
