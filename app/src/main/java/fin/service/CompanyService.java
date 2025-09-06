package fin.service;

import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.config.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompanyService {
    private final String dbUrl;
    
    public CompanyService(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        // Skip table creation if using PostgreSQL - tables are created via migration script
        if (DatabaseConfig.isUsingPostgreSQL()) {
            System.out.println("📊 Using PostgreSQL - schema already exists");
            return;
        }
        
        // SQLite table creation (for testing only)
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            
            // Create companies table
            stmt.execute("CREATE TABLE IF NOT EXISTS companies (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name VARCHAR(255) NOT NULL," +
                    "registration_number VARCHAR(50)," +
                    "tax_number VARCHAR(50)," +
                    "address TEXT," +
                    "contact_email VARCHAR(255)," +
                    "contact_phone VARCHAR(50)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            
            // Create fiscal_periods table
            stmt.execute("CREATE TABLE IF NOT EXISTS fiscal_periods (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "company_id INTEGER NOT NULL," +
                    "period_name VARCHAR(100) NOT NULL," +
                    "start_date DATE NOT NULL," +
                    "end_date DATE NOT NULL," +
                    "is_closed BOOLEAN DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (company_id) REFERENCES companies(id)" +
                    ")");
            
            // Create bank_accounts table
            stmt.execute("CREATE TABLE IF NOT EXISTS bank_accounts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "company_id INTEGER NOT NULL," +
                    "account_number VARCHAR(50) NOT NULL," +
                    "account_name VARCHAR(255) NOT NULL," +
                    "account_type VARCHAR(50)," +
                    "bank_name VARCHAR(100)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (company_id) REFERENCES companies(id)" +
                    ")");
            
            System.out.println("📊 SQLite tables initialized");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    public Company createCompany(Company company) {
        String sql = "INSERT INTO companies (name, registration_number, tax_number, address, " +
                "contact_email, contact_phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, company.getName());
            pstmt.setString(2, company.getRegistrationNumber());
            pstmt.setString(3, company.getTaxNumber());
            pstmt.setString(4, company.getAddress());
            pstmt.setString(5, company.getContactEmail());
            pstmt.setString(6, company.getContactPhone());
            pstmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            
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
            
            pstmt.setLong(1, fiscalPeriod.getCompanyId());
            pstmt.setString(2, fiscalPeriod.getPeriodName());
            pstmt.setDate(3, Date.valueOf(fiscalPeriod.getStartDate()));
            pstmt.setDate(4, Date.valueOf(fiscalPeriod.getEndDate()));
            pstmt.setBoolean(5, fiscalPeriod.isClosed());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            
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
            
            pstmt.setLong(1, companyId);
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
            
            pstmt.setLong(1, id);
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
    
    public FiscalPeriod getFiscalPeriodById(Long id) {
        String sql = "SELECT * FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
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
            
            pstmt.setLong(1, companyId);
            pstmt.setString(2, periodName);
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
