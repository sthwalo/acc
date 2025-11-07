package fin;

import fin.service.DepreciationService;
import fin.repository.DepreciationRepository;
import fin.config.DatabaseConfig;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

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

    @BeforeAll
    public static void setUpClass() throws Exception {
        // Setup test database schema and data
        TestConfiguration.setupTestDatabase();
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Use test database configuration
        String dbUrl = TestConfiguration.TEST_DB_URL;
        String dbUser = TestConfiguration.TEST_DB_USER;
        String dbPassword = TestConfiguration.TEST_DB_PASSWORD;

        // Initialize database connection using test database
        conn = DatabaseConfig.getTestConnection(dbUrl, dbUser, dbPassword);

        // Initialize repository and service with test database URL
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