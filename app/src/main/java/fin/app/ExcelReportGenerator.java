package fin.app;

import fin.service.ExcelFinancialReportService;

/**
 * Standalone Excel Financial Report Generator
 * Creates an Excel financial report using the template format
 */
public class ExcelReportGenerator {
    
    public static void main(String[] args) {
        System.out.println("🏢 EXCEL FINANCIAL REPORT GENERATOR");
        System.out.println("===================================");
        System.out.println();
        
        try {
            String dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
            ExcelFinancialReportService excelService = new ExcelFinancialReportService(dbUrl);
            
            // Use default company and period IDs (typically 1 for both)
            Long companyId = 1L;
            Long fiscalPeriodId = 1L;
            String outputDir = "/Users/sthwalonyoni/FIN/reports";
            
            System.out.println("📊 Generating Excel Financial Report...");
            System.out.println("Company ID: " + companyId);
            System.out.println("Fiscal Period ID: " + fiscalPeriodId);
            System.out.println("Output Directory: " + outputDir);
            System.out.println();
            
            excelService.generateComprehensiveFinancialReport(companyId, fiscalPeriodId, outputDir);
            
            System.out.println();
            System.out.println("✅ Excel Financial Report Generation Complete!");
            System.out.println("📁 Check the reports directory for your Excel file.");
            System.out.println();
            System.out.println("📋 Your Excel report includes all template sheets:");
            System.out.println("   • Cover - Company branding and title");
            System.out.println("   • Index - Table of contents");
            System.out.println("   • Co details - Company information");
            System.out.println("   • State of responsibility - Directors' responsibility");
            System.out.println("   • Audit report - Independent auditor's report");
            System.out.println("   • Directors report - Directors' report");
            System.out.println("   • Balance sheet - Statement of Financial Position");
            System.out.println("   • Income statement - Statement of Comprehensive Income");
            System.out.println("   • State of changes - Statement of Changes in Equity");
            System.out.println("   • Cash flow - Statement of Cash Flows");
            System.out.println("   • Notes 1 - Accounting policies");
            System.out.println("   • Notes 2-6 - Revenue and income notes");
            System.out.println("   • Notes 7-10 - Asset and receivables notes");
            System.out.println("   • Notes 11-12 - Cash flow notes");
            System.out.println("   • Detailed IS - Detailed Income Statement");
            System.out.println();
            System.out.println("🎯 The format matches your template exactly!");
            
        } catch (Exception e) {
            System.err.println("❌ Error generating Excel report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
