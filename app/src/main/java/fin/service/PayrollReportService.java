package fin.service;

import fin.model.Company;
import fin.model.Employee;
import fin.model.Payslip;
import fin.model.PayrollPeriod;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Payroll Report Service for FIN Financial Management System
 * Handles generation of payroll reports in PDF format for tax filing and compliance purposes
 * Separated from PayrollService to maintain single responsibility principle
 */
public class PayrollReportService {
    private final String dbUrl;

    // PDF Layout Constants
    private static final float PAGE_MARGIN_LEFT = 50f;
    private static final float PAGE_WIDTH = 550f;
    private static final float PAGE_HEIGHT = 750f;

    // Font Sizes
    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_HEADER = 14f;
    private static final float FONT_SIZE_NORMAL = 12f;
    private static final float FONT_SIZE_SMALL = 10f;
    private static final float FONT_SIZE_TINY = 8f;

    // Vertical Spacing
    private static final float LINE_SPACING_LARGE = 20f;
    private static final float LINE_SPACING_NORMAL = 15f;
    private static final float LINE_SPACING_SMALL = 12f;

    // Section Positions
    private static final float TITLE_Y = 750f;
    private static final float HEADER_Y = 720f;
    private static final float SUMMARY_Y = 650f;
    private static final float TABLE_Y = 620f;
    private static final float TAX_INFO_Y = 450f;
    private static final float YEARLY_SUMMARY_Y = 630f;
    private static final float MONTHLY_BREAKDOWN_Y = 550f;
    private static final float TAX_CERTIFICATE_Y = 200f;

    // Content Limits
    private static final int MAX_MONTHLY_PAYSLIPS = 10;
    private static final float MIN_Y_POSITION = 100f;

    // EMP201 Report Layout Constants
    private static final float EMP201_TITLE_Y = 750f;
    private static final float EMP201_COMPANY_INFO_Y = 720f;
    private static final float EMP201_TAX_TOTALS_Y = 660f;
    private static final float EMP201_TAX_DATA_Y = 630f;
    private static final float EMP201_SUMMARY_Y = 500f;
    private static final float EMP201_SUMMARY_DATA_Y = 470f;
    private static final float EMP201_FOOTER_Y = 100f;
    
    // EMP201 Spacing Constants
    private static final float EMP201_LINE_SPACING = 15f;
    private static final float EMP201_TAX_LINE_SPACING = 20f;
    private static final float EMP201_SUMMARY_LINE_SPACING = 20f;
    private static final float EMP201_FOOTER_LINE_SPACING = 12f;
    
    // EMP201 Font Sizes
    private static final float EMP201_TITLE_FONT_SIZE = 16f;
    private static final float EMP201_HEADER_FONT_SIZE = 12f;
    private static final float EMP201_SECTION_FONT_SIZE = 14f;
    private static final float EMP201_DATA_FONT_SIZE = 11f;
    private static final float EMP201_FOOTER_FONT_SIZE = 9f;
    
    // EMP201 Column Widths and Positions
    private static final float EMP201_LABEL_COLUMN_WIDTH = 50f;
    private static final float EMP201_VALUE_COLUMN_WIDTH = 100f;
    private static final float EMP201_TOTAL_LABEL_WIDTH = 470f;

    public PayrollReportService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }

    /**
     * Generate a payroll summary report in PDF format for employer tax filing purposes
     * This report provides consolidated payroll expense data across all processed periods
     */
    public void generatePayrollSummaryReport(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during test mode (build process)
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "payroll_summary_report_" + companyId + "_" + timestamp + ".pdf";
        Path reportPath = Paths.get("reports", fileName);

        // Ensure reports directory exists
        Files.createDirectories(reportPath.getParent());

        // Get company information - guaranteed non-null
        Company company = getCompanyByIdRequired(companyId);
        
        // Get summary data
        PayrollSummaryData summaryData = calculatePayrollSummaryData(companyId);

        // Display console summary for debugging (temporarily)
        System.out.println("PAYROLL SUMMARY REPORT");
        System.out.println("Company: " + company.getName());
        System.out.println("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Tax Year: 2025");
        System.out.println();
        System.out.println("PAYROLL SUMMARY");
        System.out.println("Total Gross Pay: R" + summaryData.getTotalGrossPay().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total PAYE Deducted: R" + summaryData.getTotalPAYE().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total UIF Contributions: R" + summaryData.getTotalUIF().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total Other Deductions: R" + summaryData.getTotalOtherDeductions().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total Net Pay: R" + summaryData.getTotalNetPay().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Total Employees: " + summaryData.getTotalEmployees());
        System.out.println("Number of Payroll Periods: " + summaryData.getPeriodCount());
        System.out.println();
        System.out.println("TAX FILING INFORMATION");
        System.out.println("This report summarizes payroll expenses for SARS tax filing purposes.");
        System.out.println("PAYE amounts should be declared on EMP201 submission.");
        System.out.println("UIF contributions should be declared separately.");
        System.out.println("Generated using FIN Financial Management System.");

        // Generate PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createSummaryReportHeader(contentStream, company);
                createSummaryTable(contentStream, summaryData);
                createTaxFilingInfo(contentStream);
            }

            document.save(reportPath.toFile());
        }

        System.out.println("✅ Payroll summary report generated: " + reportPath.toAbsolutePath());
    }

    private void createSummaryReportHeader(PDPageContentStream contentStream, Company company) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TITLE_Y);
        contentStream.showText("PAYROLL SUMMARY REPORT");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, HEADER_Y);
        contentStream.showText("Company: " + company.getName());
        contentStream.newLineAtOffset(0, -LINE_SPACING_LARGE);
        contentStream.showText("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        contentStream.newLineAtOffset(0, -LINE_SPACING_LARGE);
        contentStream.showText("Tax Year: 2025");
        contentStream.endText();
    }

    private void createSummaryTable(PDPageContentStream contentStream, PayrollSummaryData summaryData) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, SUMMARY_Y);
        contentStream.showText("PAYROLL SUMMARY");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TABLE_Y);
        contentStream.showText("Total Gross Pay: R" + summaryData.getTotalGrossPay().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total PAYE Deducted: R" + summaryData.getTotalPAYE().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total UIF Contributions: R" + summaryData.getTotalUIF().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total Other Deductions: R" + summaryData.getTotalOtherDeductions().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total Net Pay: R" + summaryData.getTotalNetPay().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_LARGE);
        contentStream.showText("Total Employees: " + summaryData.getTotalEmployees());
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Number of Payroll Periods: " + summaryData.getPeriodCount());
        contentStream.endText();
    }

    private void createTaxFilingInfo(PDPageContentStream contentStream) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TAX_INFO_Y);
        contentStream.showText("TAX FILING INFORMATION");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TAX_INFO_Y - LINE_SPACING_NORMAL);
        contentStream.showText("This report summarizes payroll expenses for SARS tax filing purposes.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("PAYE amounts should be declared on EMP201 submission.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("UIF contributions should be declared separately.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Generated using FIN Financial Management System.");
        contentStream.endText();
    }

    private PayrollSummaryData calculatePayrollSummaryData(Long companyId) throws SQLException {
        // Get all processed payroll periods for the company
        List<PayrollPeriod> processedPeriods = getProcessedPayrollPeriods(companyId);
        
        // Debug: Check if we have any processed periods
        System.out.println("🔍 DEBUG: Found " + processedPeriods.size() + " processed payroll periods for company " + companyId);

        // Calculate summary data
        BigDecimal totalGrossPay = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNetPay = BigDecimal.ZERO;
        int totalEmployees = 0;

        for (PayrollPeriod period : processedPeriods) {
            totalGrossPay = totalGrossPay.add(period.getTotalGrossPay() != null ? period.getTotalGrossPay() : BigDecimal.ZERO);
            totalDeductions = totalDeductions.add(period.getTotalDeductions() != null ? period.getTotalDeductions() : BigDecimal.ZERO);
            totalNetPay = totalNetPay.add(period.getTotalNetPay() != null ? period.getTotalNetPay() : BigDecimal.ZERO);
            totalEmployees = Math.max(totalEmployees, period.getEmployeeCount());
        }

        // Get detailed breakdown from payslips
        String payslipQuery = """
            SELECT
                SUM(paye_tax) as total_paye,
                SUM(uif_employee) as total_uif_employee,
                SUM(uif_employer) as total_uif_employer
            FROM payslips
            WHERE company_id = ?
            """;

        BigDecimal totalPAYE = BigDecimal.ZERO;
        BigDecimal totalUIF = BigDecimal.ZERO;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(payslipQuery)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                totalPAYE = rs.getBigDecimal("total_paye") != null ? rs.getBigDecimal("total_paye") : BigDecimal.ZERO;
                BigDecimal totalUIFEmployee = rs.getBigDecimal("total_uif_employee") != null ? rs.getBigDecimal("total_uif_employee") : BigDecimal.ZERO;
                BigDecimal totalUIFEmployer = rs.getBigDecimal("total_uif_employer") != null ? rs.getBigDecimal("total_uif_employer") : BigDecimal.ZERO;
                totalUIF = totalUIFEmployee.add(totalUIFEmployer);
                
                // Debug: Check payslip data
                System.out.println("🔍 DEBUG: Payslip totals - PAYE: R" + totalPAYE + ", UIF Employee: R" + totalUIFEmployee + ", UIF Employer: R" + totalUIFEmployer);
            } else {
                System.out.println("🔍 DEBUG: No payslip data found for company " + companyId);
            }
        }

        BigDecimal totalOtherDeductions = totalDeductions.subtract(totalPAYE).subtract(totalUIF);

        return new PayrollSummaryData(totalGrossPay, totalPAYE, totalUIF, totalOtherDeductions, totalNetPay, totalEmployees, processedPeriods.size());
    }

    private static class PayrollSummaryData {
        private final BigDecimal totalGrossPay;
        private final BigDecimal totalPAYE;
        private final BigDecimal totalUIF;
        private final BigDecimal totalOtherDeductions;
        private final BigDecimal totalNetPay;
        private final int totalEmployees;
        private final int periodCount;

        PayrollSummaryData(BigDecimal valueTotalGrossPay, BigDecimal valueTotalPAYE, BigDecimal valueTotalUIF,
                          BigDecimal valueTotalOtherDeductions, BigDecimal valueTotalNetPay,
                          int valueTotalEmployees, int valuePeriodCount) {
            this.totalGrossPay = valueTotalGrossPay;
            this.totalPAYE = valueTotalPAYE;
            this.totalUIF = valueTotalUIF;
            this.totalOtherDeductions = valueTotalOtherDeductions;
            this.totalNetPay = valueTotalNetPay;
            this.totalEmployees = valueTotalEmployees;
            this.periodCount = valuePeriodCount;
        }

        public BigDecimal getTotalGrossPay() {
            return totalGrossPay;
        }

        public BigDecimal getTotalPAYE() {
            return totalPAYE;
        }

        public BigDecimal getTotalUIF() {
            return totalUIF;
        }

        public BigDecimal getTotalOtherDeductions() {
            return totalOtherDeductions;
        }

        public BigDecimal getTotalNetPay() {
            return totalNetPay;
        }

        public int getTotalEmployees() {
            return totalEmployees;
        }

        public int getPeriodCount() {
            return periodCount;
        }
    }

    /**
     * Generate individual employee payroll reports in PDF format for tax filing purposes
     * Each employee gets a detailed report of their payroll history for the year
     */
    public void generateEmployeePayrollReport(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during test mode (build process)
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // Get company information - guaranteed non-null
        Company company = getCompanyByIdRequired(companyId);

        // Get all active employees
        List<Employee> employees = getActiveEmployees(companyId);

        for (Employee employee : employees) {
            String fileName = "employee_payroll_report_" + employee.getEmployeeNumber() + "_" + timestamp + ".pdf";
            Path reportPath = Paths.get("reports", "employees", fileName);

            // Ensure reports/employees directory exists
            Files.createDirectories(reportPath.getParent());

            // Get employee's data
            EmployeePayrollData employeeData = calculateEmployeePayrollData(employee);

            // Generate PDF
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    createEmployeeReportHeader(contentStream, company, employee);
                    createYearlySummary(contentStream, employeeData);
                    createMonthlyBreakdown(contentStream, employeeData);
                    createTaxCertificateInfo(contentStream);
                }

                document.save(reportPath.toFile());
            }

            System.out.println("✅ Employee payroll report generated for " + employee.getFullName() + ": " + reportPath.toAbsolutePath());
        }

        System.out.println("✅ Generated payroll reports for " + employees.size() + " employees");
    }

    private void createEmployeeReportHeader(PDPageContentStream contentStream, Company company, Employee employee) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADER);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TITLE_Y);
        contentStream.showText("EMPLOYEE PAYROLL REPORT - TAX YEAR 2025");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, HEADER_Y);
        contentStream.showText("Company: " + company.getName());
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Employee: " + employee.getFullName());
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Employee Number: " + employee.getEmployeeNumber());
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Tax Number: " + (employee.getTaxNumber() != null ? employee.getTaxNumber() : "Not provided"));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        contentStream.endText();
    }

    private void createYearlySummary(PDPageContentStream contentStream, EmployeePayrollData employeeData) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, SUMMARY_Y);
        contentStream.showText("YEARLY SUMMARY");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, YEARLY_SUMMARY_Y);
        contentStream.showText("Total Gross Salary: R" + employeeData.getYearlyGross().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total PAYE Deducted: R" + employeeData.getYearlyPAYE().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total UIF Deducted: R" + employeeData.getYearlyUIF().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Total Net Pay: R" + employeeData.getYearlyNet().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Number of Payslips: " + employeeData.getPayslips().size());
        contentStream.endText();
    }

    private void createMonthlyBreakdown(PDPageContentStream contentStream, EmployeePayrollData employeeData) throws IOException {
        if (employeeData.getPayslips().size() > MAX_MONTHLY_PAYSLIPS) {
            return; // Skip monthly breakdown if too many payslips
        }

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, MONTHLY_BREAKDOWN_Y);
        contentStream.showText("MONTHLY BREAKDOWN");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_TINY);
        float yPos = MONTHLY_BREAKDOWN_Y - LINE_SPACING_NORMAL;

        for (Payslip payslip : employeeData.getPayslips()) {
            if (yPos < MIN_Y_POSITION) {
                break; // Prevent overflow
            }

            contentStream.beginText();
            contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, yPos);
            contentStream.showText(payslip.getPayslipNumber() + ": Gross R" +
                                 payslip.getGrossSalary().setScale(2, RoundingMode.HALF_UP) +
                                 ", PAYE R" + payslip.getPayeeTax().setScale(2, RoundingMode.HALF_UP) +
                                 ", Net R" + payslip.getNetPay().setScale(2, RoundingMode.HALF_UP));
            contentStream.endText();
            yPos -= LINE_SPACING_SMALL;
        }
    }    private void createTaxCertificateInfo(PDPageContentStream contentStream) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TAX_CERTIFICATE_Y);
        contentStream.showText("TAX CERTIFICATE INFORMATION");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_TINY);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TAX_CERTIFICATE_Y - LINE_SPACING_NORMAL);
        contentStream.showText("This report contains your payroll information for tax filing purposes.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_SMALL);
        contentStream.showText("PAYE amounts have been deducted and remitted to SARS.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_SMALL);
        contentStream.showText("UIF contributions have been made to the Unemployment Insurance Fund.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_SMALL);
        contentStream.showText("Generated using FIN Financial Management System.");
        contentStream.endText();
    }

    private EmployeePayrollData calculateEmployeePayrollData(Employee employee) throws SQLException {
        // Get employee's payslips
        List<Payslip> payslips = getEmployeePayslips(employee.getId());

        // Calculate yearly totals
        BigDecimal yearlyGross = BigDecimal.ZERO;
        BigDecimal yearlyPAYE = BigDecimal.ZERO;
        BigDecimal yearlyUIF = BigDecimal.ZERO;
        BigDecimal yearlyNet = BigDecimal.ZERO;

        for (Payslip payslip : payslips) {
            yearlyGross = yearlyGross.add(payslip.getGrossSalary() != null ? payslip.getGrossSalary() : BigDecimal.ZERO);
            yearlyPAYE = yearlyPAYE.add(payslip.getPayeeTax() != null ? payslip.getPayeeTax() : BigDecimal.ZERO);
            yearlyUIF = yearlyUIF.add(payslip.getUifEmployee() != null ? payslip.getUifEmployee() : BigDecimal.ZERO);
            yearlyNet = yearlyNet.add(payslip.getNetPay() != null ? payslip.getNetPay() : BigDecimal.ZERO);
        }

        return new EmployeePayrollData(yearlyGross, yearlyPAYE, yearlyUIF, yearlyNet, payslips);
    }

    private static class EmployeePayrollData {
        private final BigDecimal yearlyGross;
        private final BigDecimal yearlyPAYE;
        private final BigDecimal yearlyUIF;
        private final BigDecimal yearlyNet;
        private final List<Payslip> payslips;

        EmployeePayrollData(BigDecimal valueYearlyGross, BigDecimal valueYearlyPAYE, BigDecimal valueYearlyUIF,
                           BigDecimal valueYearlyNet, List<Payslip> valuePayslips) {
            this.yearlyGross = valueYearlyGross;
            this.yearlyPAYE = valueYearlyPAYE;
            this.yearlyUIF = valueYearlyUIF;
            this.yearlyNet = valueYearlyNet;
            this.payslips = valuePayslips;
        }

        public BigDecimal getYearlyGross() {
            return yearlyGross;
        }

        public BigDecimal getYearlyPAYE() {
            return yearlyPAYE;
        }

        public BigDecimal getYearlyUIF() {
            return yearlyUIF;
        }

        public BigDecimal getYearlyNet() {
            return yearlyNet;
        }

        public List<Payslip> getPayslips() {
            return payslips;
        }
    }

    // Helper methods
    private Company getCompanyByIdRequired(Long companyId) throws SQLException {
        Company company = getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        return company;
    }
    
    private Company getCompanyById(Long companyId) throws SQLException {
        String sql = "SELECT id, name, registration_number FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setRegistrationNumber(rs.getString("registration_number"));
                return company;
            }
        }
        
        // Return null if not found - the calling method will handle this
        return null;
    }

    private List<PayrollPeriod> getProcessedPayrollPeriods(Long companyId) throws SQLException {
        List<PayrollPeriod> periods = new ArrayList<>();
        String sql = "SELECT * FROM payroll_periods WHERE company_id = ? AND status = 'PROCESSED'";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PayrollPeriod period = new PayrollPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setTotalGrossPay(rs.getBigDecimal("total_gross_pay"));
                period.setTotalDeductions(rs.getBigDecimal("total_deductions"));
                period.setTotalNetPay(rs.getBigDecimal("total_net_pay"));
                period.setEmployeeCount(rs.getInt("employee_count"));
                periods.add(period);
            }
        }
        
        return periods;
    }

    private List<Employee> getActiveEmployees(Long companyId) throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT id, employee_number, first_name, last_name, tax_number FROM employees WHERE company_id = ? AND is_active = TRUE";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Employee employee = new Employee();
                employee.setId(rs.getLong("id"));
                employee.setEmployeeNumber(rs.getString("employee_number"));
                employee.setFirstName(rs.getString("first_name"));
                employee.setLastName(rs.getString("last_name"));
                employee.setTaxNumber(rs.getString("tax_number"));
                employees.add(employee);
            }
        }
        
        return employees;
    }

    private List<Payslip> getEmployeePayslips(Long employeeId) throws SQLException {
        List<Payslip> payslips = new ArrayList<>();
        String sql = "SELECT payslip_number, gross_salary, paye_tax, uif_employee, net_pay FROM payslips WHERE employee_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Payslip payslip = new Payslip();
                payslip.setPayslipNumber(rs.getString("payslip_number"));
                payslip.setGrossSalary(rs.getBigDecimal("gross_salary"));
                payslip.setPayeeTax(rs.getBigDecimal("paye_tax"));
                payslip.setUifEmployee(rs.getBigDecimal("uif_employee"));
                payslip.setNetPay(rs.getBigDecimal("net_pay"));
                payslips.add(payslip);
            }
        }
        
        return payslips;
    }

    /**
     * Generate EMP 201 report for SARS tax submission
     * Shows totals for PAYE, UIF (employee and employer), and SDL
     */
    public void generateEMP201Report(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during test mode (build process)
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "emp201_report_" + companyId + "_" + timestamp + ".pdf";
        Path reportPath = Paths.get("reports", fileName);

        // Ensure reports directory exists
        Files.createDirectories(reportPath.getParent());

        // Get company information - guaranteed non-null
        Company company = getCompanyByIdRequired(companyId);

        // Calculate EMP 201 totals
        EMP201Data emp201Data = calculateEMP201Data(companyId);

        // Display console summary for debugging
        System.out.println("===============================================");
        System.out.println("EMP 201 REPORT - MONTHLY EMPLOYER DECLARATIONS");
        System.out.println("===============================================");
        System.out.println("Company: " + company.getName());
        System.out.println("Company Registration: " + (company.getRegistrationNumber() != null ? company.getRegistrationNumber() : "Not set"));
        System.out.println("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("-----------------------------------------------");
        System.out.println("TAX TOTALS FOR REPORTING PERIOD:");
        System.out.println("PAYE (Pay As You Earn): R" + String.format("%.2f", emp201Data.totalPAYE));
        System.out.println("UIF Employee Contributions (1%): R" + String.format("%.2f", emp201Data.totalUIFEmployee));
        System.out.println("UIF Employer Contributions (1%): R" + String.format("%.2f", emp201Data.totalUIFEmployer));
        System.out.println("Total UIF: R" + String.format("%.2f", emp201Data.totalUIFEmployee + emp201Data.totalUIFEmployer));
        System.out.println("SDL (Skills Development Levy): R" + String.format("%.2f", emp201Data.totalSDL));
        System.out.println("-----------------------------------------------");
        System.out.println("SUMMARY:");
        System.out.println("Total Employees: " + emp201Data.totalEmployees);
        System.out.println("Total Payroll Periods: " + emp201Data.totalPeriods);
        System.out.println("Total Gross Pay: R" + String.format("%.2f", emp201Data.totalGrossPay));
        System.out.println("Total Tax Liability: R" + String.format("%.2f", 
            emp201Data.totalPAYE + emp201Data.totalUIFEmployee + emp201Data.totalUIFEmployer + emp201Data.totalSDL));
        System.out.println("===============================================");

        // Generate PDF report
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Title
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), EMP201_TITLE_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_TITLE_Y);
                contentStream.showText("EMP 201 - MONTHLY EMPLOYER DECLARATIONS");
                contentStream.endText();

                // Company info
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), EMP201_HEADER_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_COMPANY_INFO_Y);
                contentStream.showText("Company: " + company.getName());
                contentStream.newLineAtOffset(0, -EMP201_LINE_SPACING);
                contentStream.showText("Registration: " + (company.getRegistrationNumber() != null ? company.getRegistrationNumber() : "Not set"));
                contentStream.newLineAtOffset(0, -EMP201_LINE_SPACING);
                contentStream.showText("Report Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                contentStream.endText();

                // Tax totals section
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), EMP201_SECTION_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_TAX_TOTALS_Y);
                contentStream.showText("TAX TOTALS FOR REPORTING PERIOD");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), EMP201_DATA_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_TAX_DATA_Y);
                contentStream.showText("PAYE (Pay As You Earn): R" + String.format("%,.2f", emp201Data.totalPAYE));
                contentStream.newLineAtOffset(0, -EMP201_TAX_LINE_SPACING);
                contentStream.showText("UIF Employee Contributions (1%): R" + String.format("%,.2f", emp201Data.totalUIFEmployee));
                contentStream.newLineAtOffset(0, -EMP201_TAX_LINE_SPACING);
                contentStream.showText("UIF Employer Contributions (1%): R" + String.format("%,.2f", emp201Data.totalUIFEmployer));
                contentStream.newLineAtOffset(0, -EMP201_TAX_LINE_SPACING);
                contentStream.showText("Total UIF: R" + String.format("%,.2f", emp201Data.totalUIFEmployee + emp201Data.totalUIFEmployer));
                contentStream.newLineAtOffset(0, -EMP201_TAX_LINE_SPACING);
                contentStream.showText("SDL (Skills Development Levy): R" + String.format("%,.2f", emp201Data.totalSDL));
                contentStream.endText();

                // Summary section
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), EMP201_SECTION_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_SUMMARY_Y);
                contentStream.showText("SUMMARY");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), EMP201_DATA_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_SUMMARY_DATA_Y);
                contentStream.showText("Total Employees: " + emp201Data.totalEmployees);
                contentStream.newLineAtOffset(0, -EMP201_SUMMARY_LINE_SPACING);
                contentStream.showText("Total Payroll Periods: " + emp201Data.totalPeriods);
                contentStream.newLineAtOffset(0, -EMP201_SUMMARY_LINE_SPACING);
                contentStream.showText("Total Gross Pay: R" + String.format("%,.2f", emp201Data.totalGrossPay));
                contentStream.newLineAtOffset(0, -EMP201_SUMMARY_LINE_SPACING);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), EMP201_DATA_FONT_SIZE);
                contentStream.showText("Total Tax Liability: R" + String.format("%,.2f", 
                    emp201Data.totalPAYE + emp201Data.totalUIFEmployee + emp201Data.totalUIFEmployer + emp201Data.totalSDL));
                contentStream.endText();

                // Footer note
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), EMP201_FOOTER_FONT_SIZE);
                contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, EMP201_FOOTER_Y);
                contentStream.showText("Note: UIF contributions are calculated at 1% each for employee and employer");
                contentStream.newLineAtOffset(0, -EMP201_FOOTER_LINE_SPACING);
                contentStream.showText("SDL is calculated at 1% of total payroll for companies with payroll > R500,000 annually");
                contentStream.newLineAtOffset(0, -EMP201_FOOTER_LINE_SPACING);
                contentStream.showText("This report is for informational purposes. Consult your tax advisor for official submissions.");
                contentStream.endText();
            }

            document.save(reportPath.toFile());
        }

        System.out.println("✅ EMP 201 report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Calculate EMP 201 data - totals for PAYE, UIF, SDL
     */
    private EMP201Data calculateEMP201Data(Long companyId) throws SQLException {
        String sql = "SELECT " +
                "    COUNT(DISTINCT e.id) as total_employees, " +
                "    COUNT(DISTINCT pp.id) as total_periods, " +
                "    COALESCE(SUM(p.gross_salary), 0) as total_gross, " +
                "    COALESCE(SUM(p.paye_tax), 0) as total_paye, " +
                "    COALESCE(SUM(p.uif_employee), 0) as total_uif_employee, " +
                "    COALESCE(SUM(p.uif_employer), 0) as total_uif_employer, " +
                "    COALESCE(SUM(p.sdl_levy), 0) as total_sdl " +
                "FROM employees e " +
                "LEFT JOIN payslips p ON e.id = p.employee_id " +
                "LEFT JOIN payroll_periods pp ON p.payroll_period_id = pp.id " +
                "WHERE e.company_id = ?";

        System.out.println("🔍 DEBUG: Calculating EMP 201 data for company " + companyId);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                EMP201Data data = new EMP201Data();
                data.totalEmployees = rs.getInt("total_employees");
                data.totalPeriods = rs.getInt("total_periods");
                data.totalGrossPay = rs.getBigDecimal("total_gross").doubleValue();
                data.totalPAYE = rs.getBigDecimal("total_paye").doubleValue();
                data.totalUIFEmployee = rs.getBigDecimal("total_uif_employee").doubleValue();
                data.totalUIFEmployer = rs.getBigDecimal("total_uif_employer").doubleValue();
                data.totalSDL = rs.getBigDecimal("total_sdl").doubleValue();

                System.out.println("🔍 DEBUG: Found " + data.totalEmployees + " employees, " + data.totalPeriods + " periods");
                System.out.println("🔍 DEBUG: Total gross: R" + String.format("%.2f", data.totalGrossPay));
                System.out.println("🔍 DEBUG: PAYE: R" + String.format("%.2f", data.totalPAYE));
                System.out.println("🔍 DEBUG: UIF Employee: R" + String.format("%.2f", data.totalUIFEmployee));
                System.out.println("🔍 DEBUG: UIF Employer: R" + String.format("%.2f", data.totalUIFEmployer));
                System.out.println("🔍 DEBUG: SDL: R" + String.format("%.2f", data.totalSDL));

                return data;
            }
        }

        return new EMP201Data(); // Return empty data if no results
    }

    /**
     * EMP 201 data structure
     */
    private static class EMP201Data {
        double totalPAYE = 0.0;
        double totalUIFEmployee = 0.0;  // 1% from employee
        double totalUIFEmployer = 0.0;  // 1% from employer  
        double totalSDL = 0.0;          // Skills Development Levy
        double totalGrossPay = 0.0;
        int totalEmployees = 0;
        int totalPeriods = 0;
    }
}
