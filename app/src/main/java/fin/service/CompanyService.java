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

import fin.model.Company;
import fin.model.FiscalPeriod;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompanyService {
    private final String dbUrl;
    
    // PreparedStatement parameter indices for company operations
    private static final int PARAM_COMPANY_NAME = 1;
    private static final int PARAM_REGISTRATION_NUMBER = 2;
    private static final int PARAM_TAX_NUMBER = 3;
    private static final int PARAM_ADDRESS = 4;
    private static final int PARAM_CONTACT_EMAIL = 5;
    private static final int PARAM_CONTACT_PHONE = 6;
    private static final int PARAM_LOGO_PATH = 7;
    private static final int PARAM_CREATED_AT = 8;
    private static final int PARAM_UPDATED_AT = 8;
    private static final int PARAM_COMPANY_ID = 9;
    
    // PreparedStatement parameter indices for fiscal period operations
    private static final int PARAM_FISCAL_COMPANY_ID = 1;
    private static final int PARAM_PERIOD_NAME = 2;
    private static final int PARAM_START_DATE = 3;
    private static final int PARAM_END_DATE = 4;
    private static final int PARAM_IS_CLOSED = 5;
    private static final int PARAM_FISCAL_CREATED_AT = 6;
    
    // PreparedStatement parameter indices for query operations
    private static final int PARAM_QUERY_COMPANY_ID = 1;
    private static final int PARAM_QUERY_PERIOD_NAME = 2;
    
    public CompanyService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }
    


    public Company createCompany(Company company) {
        String sql = "INSERT INTO companies (name, registration_number, tax_number, address, " +
                "contact_email, contact_phone, logo_path, bank_name, account_number, " +
                "account_type, branch_code, vat_registered, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(PARAM_COMPANY_NAME, company.getName());
            pstmt.setString(PARAM_REGISTRATION_NUMBER, company.getRegistrationNumber());
            pstmt.setString(PARAM_TAX_NUMBER, company.getTaxNumber());
            pstmt.setString(PARAM_ADDRESS, company.getAddress());
            pstmt.setString(PARAM_CONTACT_EMAIL, company.getContactEmail());
            pstmt.setString(PARAM_CONTACT_PHONE, company.getContactPhone());
            pstmt.setString(PARAM_LOGO_PATH, company.getLogoPath());
            pstmt.setString(8, company.getBankName());
            pstmt.setString(9, company.getAccountNumber());
            pstmt.setString(10, company.getAccountType());
            pstmt.setString(11, company.getBranchCode());
            pstmt.setBoolean(12, company.isVatRegistered());
            pstmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating company failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    company.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating company failed, no ID obtained.");
                }
            }
            
            return company;
            
        } catch (SQLException e) {
            System.err.println("Error creating company: " + e.getMessage());
            throw new RuntimeException("Failed to create company", e);
        }
    }
    
    public FiscalPeriod createFiscalPeriod(FiscalPeriod fiscalPeriod) {
        String sql = "INSERT INTO fiscal_periods (company_id, period_name, start_date, end_date, " +
                "is_closed, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(PARAM_FISCAL_COMPANY_ID, fiscalPeriod.getCompanyId());
            pstmt.setString(PARAM_PERIOD_NAME, fiscalPeriod.getPeriodName());
            pstmt.setDate(PARAM_START_DATE, Date.valueOf(fiscalPeriod.getStartDate()));
            pstmt.setDate(PARAM_END_DATE, Date.valueOf(fiscalPeriod.getEndDate()));
            pstmt.setBoolean(PARAM_IS_CLOSED, fiscalPeriod.isClosed());
            pstmt.setTimestamp(PARAM_FISCAL_CREATED_AT, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating fiscal period failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fiscalPeriod.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating fiscal period failed, no ID obtained.");
                }
            }
            
            return fiscalPeriod;
            
        } catch (SQLException e) {
            System.err.println("Error creating fiscal period: " + e.getMessage());
            throw new RuntimeException("Failed to create fiscal period", e);
        }
    }
    
    public List<FiscalPeriod> getFiscalPeriodsByCompany(Long companyId) {
        String sql = "SELECT * FROM fiscal_periods WHERE company_id = ?";
        List<FiscalPeriod> periods = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(PARAM_QUERY_COMPANY_ID, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FiscalPeriod period = new FiscalPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStartDate(rs.getDate("start_date").toLocalDate());
                period.setEndDate(rs.getDate("end_date").toLocalDate());
                period.setClosed(rs.getBoolean("is_closed"));
                period.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                periods.add(period);
            }
            
            return periods;
            
        } catch (SQLException e) {
            System.err.println("Error getting fiscal periods: " + e.getMessage());
            throw new RuntimeException("Failed to get fiscal periods", e);
        }
    }
    
    public List<Company> getAllCompanies() {
        String sql = "SELECT * FROM companies";
        List<Company> companies = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setRegistrationNumber(rs.getString("registration_number"));
                company.setTaxNumber(rs.getString("tax_number"));
                company.setAddress(rs.getString("address"));
                company.setContactEmail(rs.getString("contact_email"));
                company.setContactPhone(rs.getString("contact_phone"));
                company.setLogoPath(rs.getString("logo_path"));
                company.setBankName(rs.getString("bank_name"));
                company.setAccountNumber(rs.getString("account_number"));
                company.setAccountType(rs.getString("account_type"));
                company.setBranchCode(rs.getString("branch_code"));
                company.setVatRegistered(rs.getBoolean("vat_registered"));
                company.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                companies.add(company);
            }
            
            return companies;
            
        } catch (SQLException e) {
            System.err.println("Error getting companies: " + e.getMessage());
            throw new RuntimeException("Failed to get companies", e);
        }
    }
    
    public Company getCompanyById(Long id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(PARAM_QUERY_COMPANY_ID, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setRegistrationNumber(rs.getString("registration_number"));
                company.setTaxNumber(rs.getString("tax_number"));
                company.setAddress(rs.getString("address"));
                company.setContactEmail(rs.getString("contact_email"));
                company.setContactPhone(rs.getString("contact_phone"));
                company.setLogoPath(rs.getString("logo_path"));
                company.setBankName(rs.getString("bank_name"));
                company.setAccountNumber(rs.getString("account_number"));
                company.setAccountType(rs.getString("account_type"));
                company.setBranchCode(rs.getString("branch_code"));
                company.setVatRegistered(rs.getBoolean("vat_registered"));
                company.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return company;
            } else {
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting company: " + e.getMessage());
            throw new RuntimeException("Failed to get company", e);
        }
    }
    
    /**
     * Update an existing company in the database
     * @param company The company with updated fields
     * @return The updated company
     */
    public Company updateCompany(Company company) {
        if (company.getId() == null) {
            throw new IllegalArgumentException("Company ID cannot be null for update operation");
        }
        
        String sql = "UPDATE companies SET name = ?, registration_number = ?, tax_number = ?, " +
                "address = ?, contact_email = ?, contact_phone = ?, logo_path = ?, " +
                "bank_name = ?, account_number = ?, account_type = ?, branch_code = ?, " +
                "vat_registered = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(PARAM_COMPANY_NAME, company.getName());
            pstmt.setString(PARAM_REGISTRATION_NUMBER, company.getRegistrationNumber());
            pstmt.setString(PARAM_TAX_NUMBER, company.getTaxNumber());
            pstmt.setString(PARAM_ADDRESS, company.getAddress());
            pstmt.setString(PARAM_CONTACT_EMAIL, company.getContactEmail());
            pstmt.setString(PARAM_CONTACT_PHONE, company.getContactPhone());
            pstmt.setString(PARAM_LOGO_PATH, company.getLogoPath());
            pstmt.setString(8, company.getBankName());
            pstmt.setString(9, company.getAccountNumber());
            pstmt.setString(10, company.getAccountType());
            pstmt.setString(11, company.getBranchCode());
            pstmt.setBoolean(12, company.isVatRegistered());
            pstmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(14, company.getId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating company failed, no rows affected.");
            }
            
            return getCompanyById(company.getId());
            
        } catch (SQLException e) {
            System.err.println("Error updating company: " + e.getMessage());
            throw new RuntimeException("Failed to update company", e);
        }
    }
    
    /**
     * Delete a company from the database
     * @param id The ID of the company to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteCompany(Long id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(PARAM_QUERY_COMPANY_ID, id);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting company: " + e.getMessage());
            throw new RuntimeException("Failed to delete company", e);
        }
    }
    
    public FiscalPeriod getFiscalPeriodById(Long id) {
        String sql = "SELECT * FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(PARAM_QUERY_COMPANY_ID, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FiscalPeriod period = new FiscalPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStartDate(rs.getDate("start_date").toLocalDate());
                period.setEndDate(rs.getDate("end_date").toLocalDate());
                period.setClosed(rs.getBoolean("is_closed"));
                period.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return period;
            } else {
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting fiscal period: " + e.getMessage());
            throw new RuntimeException("Failed to get fiscal period", e);
        }
    }
    
    public FiscalPeriod getFiscalPeriodByName(Long companyId, String periodName) {
        String sql = "SELECT * FROM fiscal_periods WHERE company_id = ? AND period_name = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(PARAM_QUERY_COMPANY_ID, companyId);
            pstmt.setString(PARAM_QUERY_PERIOD_NAME, periodName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FiscalPeriod period = new FiscalPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStartDate(rs.getDate("start_date").toLocalDate());
                period.setEndDate(rs.getDate("end_date").toLocalDate());
                period.setClosed(rs.getBoolean("is_closed"));
                period.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return period;
            } else {
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting fiscal period: " + e.getMessage());
            throw new RuntimeException("Failed to get fiscal period", e);
        }
    }
}
