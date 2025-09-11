package fin.app;

import fin.model.Company;
import fin.service.InteractiveClassificationService;
import fin.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Entry point for the Interactive Classification System
 * This demonstrates using the unified InteractiveClassificationService
 */
public class ClassificationApp {
    private static final Logger LOGGER = Logger.getLogger(ClassificationApp.class.getName());
    
    public static void main(String[] args) {
        String dbUrl = DatabaseConfig.getDatabaseUrl();
        InteractiveClassificationService service = new InteractiveClassificationService();
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("====================================================");
        System.out.println("🏢 INTERACTIVE TRANSACTION CLASSIFICATION SYSTEM");
        System.out.println("====================================================");
        
        // Get company
        Company company = getCompany(dbUrl, scanner);
        if (company == null) {
            System.out.println("❌ No company selected. Exiting.");
            return;
        }
        
        // Get fiscal period
        Long fiscalPeriodId = getFiscalPeriod(dbUrl, company.getId(), scanner);
        if (fiscalPeriodId == null) {
            System.out.println("❌ No fiscal period selected. Exiting.");
            return;
        }
        
        // Run the interactive categorization
        System.out.println("\n🏢 Starting classification for " + company.getName());
        service.runInteractiveCategorization(company.getId(), fiscalPeriodId);
        
        System.out.println("\n👋 Thank you for using the Interactive Classification System");
    }
    
    /**
     * Prompt user to select a company
     */
    private static Company getCompany(String dbUrl, Scanner scanner) {
        System.out.println("\n🏢 Available Companies:");
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, name FROM companies ORDER BY name")) {
            
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
                System.out.println(count + ". " + rs.getString("name"));
            }
            
            if (count == 0) {
                System.out.println("❌ No companies found in database.");
                return null;
            }
            
            System.out.print("\nSelect company (1-" + count + "): ");
            int selection = Integer.parseInt(scanner.nextLine().trim());
            
            if (selection < 1 || selection > count) {
                System.out.println("❌ Invalid selection.");
                return null;
            }
            
            // Get the selected company
            rs = stmt.executeQuery();
            for (int i = 1; i <= selection; i++) {
                rs.next();
            }
            
            Company company = new Company();
            company.setId(rs.getLong("id"));
            company.setName(rs.getString("name"));
            
            System.out.println("✅ Selected: " + company.getName());
            
            return company;
            
        } catch (SQLException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error selecting company", e);
            return null;
        }
    }
    
    /**
     * Prompt user to select a fiscal period
     */
    private static Long getFiscalPeriod(String dbUrl, Long companyId, Scanner scanner) {
        System.out.println("\n📅 Available Fiscal Periods:");
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, name, start_date, end_date FROM fiscal_periods " +
                 "WHERE company_id = ? ORDER BY start_date DESC")) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
                System.out.printf("%d. %s (%s to %s)%n", 
                               count, 
                               rs.getString("name"),
                               rs.getDate("start_date"),
                               rs.getDate("end_date"));
            }
            
            if (count == 0) {
                System.out.println("❌ No fiscal periods found for this company.");
                return null;
            }
            
            System.out.print("\nSelect fiscal period (1-" + count + "): ");
            int selection = Integer.parseInt(scanner.nextLine().trim());
            
            if (selection < 1 || selection > count) {
                System.out.println("❌ Invalid selection.");
                return null;
            }
            
            // Get the selected fiscal period
            rs = stmt.executeQuery();
            for (int i = 1; i <= selection; i++) {
                rs.next();
            }
            
            Long fiscalPeriodId = rs.getLong("id");
            System.out.println("✅ Selected: " + rs.getString("name"));
            
            return fiscalPeriodId;
            
        } catch (SQLException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error selecting fiscal period", e);
            return null;
        }
    }
}
