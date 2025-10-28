import fin.service.BudgetReportService;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestReport {
    public static void main(String[] args) throws Exception {
        Properties env = new Properties();
        env.load(Files.newBufferedReader(Paths.get(".env")));
        String dbUrl = env.getProperty("DATABASE_URL");
        
        BudgetReportService service = new BudgetReportService(dbUrl);
        service.generateStrategicPlanReport(1L);
        System.out.println("Strategic plan report generated!");
    }
}
