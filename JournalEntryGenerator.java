import fin.service.TransactionMappingService;
import java.sql.*;

public class JournalEntryGenerator {
    public static void main(String[] args) {
        String dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";

        try {
            // Get company ID for Xinghizana Group
            long companyId = getCompanyId(dbUrl, "Xinghizana Group");

            if (companyId == -1) {
                System.err.println("Company not found!");
                return;
            }

            // Create service and generate journal entries
            TransactionMappingService service = new TransactionMappingService(dbUrl);
            int created = service.generateJournalEntriesForUnclassifiedTransactions(companyId);

            System.out.println("✅ Created " + created + " journal entries successfully!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static long getCompanyId(String dbUrl, String companyName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT id FROM companies WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, companyName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }
        }
        return -1;
    }
}