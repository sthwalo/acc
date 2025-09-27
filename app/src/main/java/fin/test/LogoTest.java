package fin.test;

import fin.repository.CompanyRepository;
import fin.model.Company;
import fin.config.DatabaseConfig;
import java.util.Optional;

/**
 * Simple test to verify logo loading functionality
 */
public class LogoTest {
    public static void main(String[] args) {
        try {
            String dbUrl = DatabaseConfig.getDatabaseUrl();
            CompanyRepository companyRepo = new CompanyRepository(dbUrl);

            // Try to load company with ID 3 (Limelight Academy)
            Optional<Company> companyOpt = companyRepo.findById(3L);

            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                System.out.println("✅ Company loaded: " + company.getName());
                System.out.println("Logo path: " + company.getLogoPath());

                if (company.getLogoPath() != null && !company.getLogoPath().isEmpty()) {
                    System.out.println("✅ Logo path is set: " + company.getLogoPath());
                } else {
                    System.out.println("❌ Logo path is null or empty");
                }
            } else {
                System.out.println("❌ Company not found");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}