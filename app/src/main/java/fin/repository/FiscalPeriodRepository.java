package fin.repository;

import fin.model.FiscalPeriod;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for FiscalPeriod entities.
 */
public class FiscalPeriodRepository implements BaseRepository<FiscalPeriod, Long> {
    private final String dbUrl;

    // Parameter constants for PreparedStatement indices
    private static final int PARAM_COMPANY_ID = 1;
    private static final int PARAM_PERIOD_NAME = 2;
    private static final int PARAM_START_DATE = 3;
    private static final int PARAM_END_DATE = 4;
    private static final int PARAM_IS_CLOSED = 5;
    private static final int PARAM_ID_UPDATE = 6;

    public FiscalPeriodRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public FiscalPeriod save(FiscalPeriod period) {
        String sql = period.getId() == null ?
            "INSERT INTO fiscal_periods (company_id, period_name, start_date, end_date, is_closed) VALUES (?, ?, ?, ?, ?)" :
            "UPDATE fiscal_periods SET company_id = ?, period_name = ?, start_date = ?, end_date = ?, is_closed = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(PARAM_COMPANY_ID, period.getCompanyId());
            stmt.setString(PARAM_PERIOD_NAME, period.getPeriodName());
            stmt.setDate(PARAM_START_DATE, Date.valueOf(period.getStartDate()));
            stmt.setDate(PARAM_END_DATE, Date.valueOf(period.getEndDate()));
            stmt.setBoolean(PARAM_IS_CLOSED, period.isClosed());
            
            if (period.getId() != null) {
                stmt.setLong(PARAM_ID_UPDATE, period.getId());
            }

            stmt.executeUpdate();

            if (period.getId() == null) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    period.setId(rs.getLong(1));
                }
            }

            return period;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving fiscal period", e);
        }
    }

    @Override
    public Optional<FiscalPeriod> findById(Long id) {
        String sql = "SELECT * FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToFiscalPeriod(rs));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding fiscal period by id", e);
        }
    }

    @Override
    public List<FiscalPeriod> findAll() {
        String sql = "SELECT * FROM fiscal_periods";
        List<FiscalPeriod> periods = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                periods.add(mapResultSetToFiscalPeriod(rs));
            }
            
            return periods;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all fiscal periods", e);
        }
    }

    @Override
    public void delete(FiscalPeriod period) {
        if (period.getId() != null) {
            deleteById(period.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting fiscal period", e);
        }
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking fiscal period existence", e);
        }
    }

    private FiscalPeriod mapResultSetToFiscalPeriod(ResultSet rs) throws SQLException {
        FiscalPeriod period = new FiscalPeriod();
        period.setId(rs.getLong("id"));
        period.setCompanyId(rs.getLong("company_id"));
        period.setPeriodName(rs.getString("period_name"));
        period.setStartDate(rs.getDate("start_date").toLocalDate());
        period.setEndDate(rs.getDate("end_date").toLocalDate());
        period.setClosed(rs.getBoolean("is_closed"));
        return period;
    }

    // Additional query methods
    public List<FiscalPeriod> findByCompanyId(Long companyId) {
        String sql = "SELECT * FROM fiscal_periods WHERE company_id = ?";
        List<FiscalPeriod> periods = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                periods.add(mapResultSetToFiscalPeriod(rs));
            }
            
            return periods;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding fiscal periods by company", e);
        }
    }

    public List<FiscalPeriod> findActiveByCompanyId(Long companyId) {
        String sql = "SELECT * FROM fiscal_periods WHERE company_id = ? AND is_closed = false";
        List<FiscalPeriod> periods = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                periods.add(mapResultSetToFiscalPeriod(rs));
            }
            
            return periods;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active fiscal periods by company", e);
        }
    }
}
