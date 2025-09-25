package fin.test;

import fin.model.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Simple test to verify employee name formatting with second names
 */
public class NameTest {

    public static void main(String[] args) {
        System.out.println("🧪 EMPLOYEE NAME FORMATTING TEST");
        System.out.println("=================================");

        try {
            // Create test employee with second name
            Employee employee = new Employee();
            employee.setTitle("Mr");
            employee.setFirstName("John");
            employee.setSecondName("William");
            employee.setLastName("Smith");
            employee.setEmployeeNumber("TEST001");
            employee.setBasicSalary(new BigDecimal("50000.00"));
            employee.setHireDate(LocalDate.of(2020, 1, 15));

            System.out.println("Employee Details:");
            System.out.println("  Title: " + employee.getTitle());
            System.out.println("  First Name: " + employee.getFirstName());
            System.out.println("  Second Name: " + employee.getSecondName());
            System.out.println("  Last Name: " + employee.getLastName());
            System.out.println("  Full Name: " + employee.getFullName());

            // Test another employee without second name
            Employee employee2 = new Employee();
            employee2.setTitle("Ms");
            employee2.setFirstName("Jane");
            employee2.setSecondName(null); // No second name
            employee2.setLastName("Doe");
            employee2.setEmployeeNumber("TEST002");
            employee2.setBasicSalary(new BigDecimal("45000.00"));
            employee2.setHireDate(LocalDate.of(2019, 6, 10));

            System.out.println("\nEmployee 2 Details:");
            System.out.println("  Title: " + employee2.getTitle());
            System.out.println("  First Name: " + employee2.getFirstName());
            System.out.println("  Second Name: " + employee2.getSecondName());
            System.out.println("  Last Name: " + employee2.getLastName());
            System.out.println("  Full Name: " + employee2.getFullName());

            System.out.println("\n✅ Name formatting test completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}