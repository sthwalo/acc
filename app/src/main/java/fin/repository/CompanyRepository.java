package fin.repository;

import fin.model.Company;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for Company entities.
 */
public class CompanyRepository implements BaseRepository<Company, Long> {
    private final String dbUrl;

    public CompanyRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public Company save(Company company) {
        String sql = company.getId() == null ?
            "INSERT INTO companies (name, registration_number, tax_number, address, contact_email, contact_phone) VALUES (?, ?, ?, ?, ?, ?)" :
            "UPDATE companies SET name = ?, registration_number = ?, tax_number = ?, address = ?, contact_email = ?, contact_phone = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, company.getName());
            stmt.setString(2, company.getRegistrationNumber());
            stmt.setString(3, company.getTaxNumber());
            stmt.setString(4, company.getAddress());
            stmt.setString(5, company.getContactEmail());
            stmt.setString(6, company.getContactPhone());
            
            if (company.getId() != null) {
                stmt.setLong(7, company.getId());
            }

            stmt.executeUpdate();

            if (company.getId() == null) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    company.setId(rs.getLong(1));
                }
            }

            return company;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving company", e);
        }
    }

    @Override
    public Optional<Company> findById(Long id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCompany(rs));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding company by id", e);
        }
    }

    @Override
    public List<Company> findAll() {
        String sql = "SELECT * FROM companies";
        List<Company> companies = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                companies.add(mapResultSetToCompany(rs));
            }
            
            return companies;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all companies", e);
        }
    }

    @Override
    public void delete(Company company) {
        if (company.getId() != null) {
            deleteById(company.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting company", e);
        }
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking company existence", e);
        }
    }

    private Company mapResultSetToCompany(ResultSet rs) throws SQLException {
        Company company = new Company(rs.getString("name"));
        company.setId(rs.getLong("id"));
        company.setRegistrationNumber(rs.getString("registration_number"));
        company.setTaxNumber(rs.getString("tax_number"));
        company.setAddress(rs.getString("address"));
        company.setContactEmail(rs.getString("contact_email"));
        company.setContactPhone(rs.getString("contact_phone"));
        return company;
    }

    // Additional query methods
    public Optional<Company> findByName(String name) {
        String sql = "SELECT * FROM companies WHERE name = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToCompany(rs));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding company by name", e);
        }
    }
}
