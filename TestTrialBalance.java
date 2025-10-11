import fin.service.*;
import fin.repository.*;
import com.zaxxer.hikari.*;
import java.sql.*;

public class TestTrialBalance {
    public static void main(String[] args) {
        try {
            // Database configuration
            String dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
            String dbUser = "sthwalonyoni";
            String dbPassword = "";
            
            // Create DataSource
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            HikariDataSource dataSource = new HikariDataSource(config);
            
            // Create repository
            JdbcFinancialDataRepository repository = new JdbcFinancialDataRepository(dataSource);
            
            // Create services
            GeneralLedgerService generalLedgerService = new GeneralLedgerService(repository);
            TrialBalanceService trialBalanceService = new TrialBalanceService(repository, generalLedgerService);
            
            System.out.println("🔗 Generating Trial Balance with correct GL→TB flow...");
            System.out.println();
            
            // Generate Trial Balance for Company 2, Fiscal Period 7
            String trialBalance = trialBalanceService.generateTrialBalance(2, 7);
            System.out.println(trialBalance);
            
            dataSource.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}