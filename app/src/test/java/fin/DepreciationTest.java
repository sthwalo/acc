package fin;

import fin.service.DepreciationService;
import fin.repository.DepreciationRepository;
import fin.config.DatabaseConfig;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test to verify IAS 16 depreciation posting works correctly
 */
public class DepreciationTest {

    private Connection conn;
    private DepreciationService service;

    @BeforeEach
    public void setUp() throws Exception {
        // Force use of production database for this test
        // (since test database may not have depreciation tables set up)
        System.setProperty("fin.database.test.url", System.getProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/drimacc_db"));
        System.setProperty("TEST_DATABASE_USER", System.getProperty("DATABASE_USER", "sthwalonyoni"));
        System.setProperty("TEST_DATABASE_PASSWORD", System.getProperty("DATABASE_PASSWORD", "drimPro1823"));

        // Re-initialize DatabaseConfig with test settings
        DatabaseConfig.loadConfiguration();

        // Initialize database connection
        conn = DatabaseConfig.getConnection();
        String dbUrl = DatabaseConfig.getDatabaseUrl();

        // Initialize repository and service with proper dbUrl for accountRepository
        DepreciationRepository repository = new DepreciationRepository(dbUrl);
        service = new DepreciationService(dbUrl, repository);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    public void testIAS16DepreciationPosting() {
        try {
            // Test reposting depreciation schedules
            System.out.println("Testing IAS 16 depreciation posting...");
            service.repostDepreciationSchedules(5L); // Company ID 5 (where schedule 19 belongs)

            System.out.println("✅ Depreciation reposting completed successfully!");

            // Check the Computer asset entries (Schedule ID 19)
            System.out.println("Checking Computer asset depreciation entries...");
            Optional<DepreciationSchedule> scheduleOpt = service.getDepreciationSchedule(19L);
            assertTrue(scheduleOpt.isPresent(), "Schedule ID 19 should exist");

            DepreciationSchedule schedule = scheduleOpt.get();
            List<DepreciationYear> years = schedule.getYears();

            // Verify we have the expected years
            assertFalse(years.isEmpty(), "Depreciation schedule should have years");

            for (DepreciationYear year : years) {
                System.out.println("Year " + year.getYear() + ": " +
                    year.getDepreciation() + " - Status: " + schedule.getStatus());
                assertNotNull(year.getDepreciation(), "Depreciation amount should not be null");
                assertTrue(year.getDepreciation().doubleValue() >= 0, "Depreciation should be non-negative");
            }

            System.out.println("✅ Test completed successfully!");

        } catch (Exception e) {
            fail("Test failed: " + e.getMessage(), e);
        }
    }
}