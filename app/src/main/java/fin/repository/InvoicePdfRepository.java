package fin.repository;

import fin.dto.InvoicePdfData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class InvoicePdfRepository {
    private final JdbcTemplate jdbc;

    public InvoicePdfRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public InvoicePdfData findInvoicePdfDataByInvoiceId(Long invoiceId) {
        final String sql = """
            SELECT mi.invoice_number, mi.invoice_date, mi.description, mi.amount,
                   da.code as debit_code, da.name as debit_name,
                   ca.code as credit_code, ca.name as credit_name
            FROM manual_invoices mi
            JOIN accounts da ON mi.debit_account_id = da.id
            JOIN accounts ca ON mi.credit_account_id = ca.id
            WHERE mi.id = ?
            """;

        try {
            return jdbc.queryForObject(sql, new Object[]{invoiceId}, (rs, rowNum) -> mapRow(rs));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private InvoicePdfData mapRow(ResultSet rs) throws SQLException {
        return new InvoicePdfData(
            rs.getString("invoice_number"),
            rs.getDate("invoice_date").toLocalDate(),
            rs.getString("description"),
            rs.getBigDecimal("amount"),
            rs.getString("debit_code"),
            rs.getString("debit_name"),
            rs.getString("credit_code"),
            rs.getString("credit_name")
        );
    }
}
