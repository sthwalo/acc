package fin.service;

import fin.config.DatabaseConfig;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple PDF service that fetches payslip data directly from database
 * and generates basic PDF without complex object mapping
 */
public class PdfPrintService {
    
    public void generateSimplePayslipPdf(int payslipId) throws SQLException, IOException {
        String sql = """
            SELECT p.*, e.first_name, e.last_name, e.employee_number, pp.period_name
            FROM payslips p
            JOIN employees e ON p.employee_id = e.id
            JOIN payroll_periods pp ON p.payroll_period_id = pp.id
            WHERE p.id = ?
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, payslipId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    createSimplePdf(rs);
                }
            }
        }
    }
    
    private void createSimplePdf(ResultSet rs) throws SQLException, IOException {
        // Create payslips directory if it doesn't exist
        Path payslipDir = Paths.get("payslips");
        Files.createDirectories(payslipDir);
        
        String fileName = "payslip_" + rs.getString("payslip_number") + "_" + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        Path filePath = payslipDir.resolve(fileName);
        
        // Use libharu to create simple PDF
        Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
        if (pdf == null) {
            throw new IOException("Failed to create PDF document");
        }
        
        try {
            // Create page
            Pointer page = Libharu.INSTANCE.HPDF_AddPage(pdf);
            Libharu.INSTANCE.HPDF_Page_SetSize(page, 0, 1); // A4
            
            // Get font
            Pointer font = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica", null);
            Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 12);
            
            // Write payslip data directly from database - each text on separate block
            float y = 800;
            
            // Payslip number
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Payslip: " + rs.getString("payslip_number"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Employee name
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Employee: " + rs.getString("first_name") + " " + rs.getString("last_name"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Employee number
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Employee Number: " + rs.getString("employee_number"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Period
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Period: " + rs.getString("period_name"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Basic Salary
            BigDecimal basicSalary = rs.getBigDecimal("basic_salary");
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Basic Salary: R" + (basicSalary != null ? basicSalary.toString() : "0.00"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Gross Salary
            BigDecimal grossSalary = rs.getBigDecimal("gross_salary");
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Gross Salary: R" + (grossSalary != null ? grossSalary.toString() : "0.00"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Total Earnings
            BigDecimal totalEarnings = rs.getBigDecimal("total_earnings");
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Total Earnings: R" + (totalEarnings != null ? totalEarnings.toString() : "0.00"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Total Deductions
            BigDecimal totalDeductions = rs.getBigDecimal("total_deductions");
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Total Deductions: R" + (totalDeductions != null ? totalDeductions.toString() : "0.00"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            y -= 20;
            
            // Net Pay
            BigDecimal netPay = rs.getBigDecimal("net_pay");
            Libharu.INSTANCE.HPDF_Page_BeginText(page);
            Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, y, "Net Pay: R" + (netPay != null ? netPay.toString() : "0.00"));
            Libharu.INSTANCE.HPDF_Page_EndText(page);
            
            // Save PDF
            Libharu.INSTANCE.HPDF_SaveToFile(pdf, filePath.toString());
            
        } finally {
            Libharu.INSTANCE.HPDF_Free(pdf);
        }
        
        System.out.println("Simple PDF generated: " + filePath);
    }
    
    public static void main(String[] args) {
        try {
            PdfPrintService service = new PdfPrintService();
            
            // Get the first payslip ID from September 2025
            String sql = "SELECT id FROM payslips ORDER BY id LIMIT 1";
        
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
             
                if (rs.next()) {
                    int payslipId = rs.getInt("id");
                    System.out.println("Generating sample PDF for September 2025 payslip ID: " + payslipId);
                    service.generateSimplePayslipPdf(payslipId);
                    System.out.println("Sample PDF generated successfully!");
                } else {
                    System.out.println("No September 2025 payslips found in database");
                }
            }
        
        } catch (Exception e) {
            System.err.println("Error generating sample PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}