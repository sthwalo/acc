package fin.repository;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC-based implementation of repository pattern providing common database operations.
 * Follows Repository pattern to eliminate SQL duplication and improve maintainability.
 */
public abstract class JdbcBaseRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcBaseRepository.class.getName());
    protected final String dbUrl;

    protected JdbcBaseRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Gets a database connection.
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Executes a SELECT query and maps results using the provided mapper.
     */
    protected <T> List<T> executeQuery(String sql, ResultSetMapper<T> mapper, Object... parameters) {
        List<T> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, parameters);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    T entity = mapper.map(rs);
                    if (entity != null) {
                        results.add(entity);
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing query: " + sql, e);
            throw new RuntimeException("Failed to execute query", e);
        }

        return results;
    }

    /**
     * Executes an INSERT/UPDATE/DELETE statement.
     */
    protected int executeUpdate(String sql, Object... parameters) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, parameters);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing update: " + sql, e);
            throw new RuntimeException("Failed to execute update", e);
        }
    }

    /**
     * Executes an INSERT statement and returns the generated key.
     */
    protected Long executeInsert(String sql, Object... parameters) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(stmt, parameters);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing insert: " + sql, e);
            throw new RuntimeException("Failed to execute insert", e);
        }

        throw new RuntimeException("Failed to get generated key");
    }

    /**
     * Sets parameters on a PreparedStatement.
     */
    private void setParameters(PreparedStatement stmt, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            stmt.setObject(i + 1, parameters[i]);
        }
    }

    /**
     * Functional interface for mapping ResultSet rows to entities.
     */
    @FunctionalInterface
    protected interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}