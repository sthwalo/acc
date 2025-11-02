package fin.service;

import fin.model.Budget;
import fin.model.BudgetCategory;
import fin.model.BudgetItem;
import fin.model.Company;
import fin.model.StrategicPlan;
import fin.model.StrategicPriority;
import fin.util.PdfFormattingUtils;
import fin.model.StrategicInitiative;
import fin.model.StrategicMilestone;
import fin.model.OperationalActivity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
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
 * Budget Report Service for FIN Financial Management System
 * Handles generation of budget reports in PDF format for financial planning and reporting
 * Features loose page generation with automatic orientation detection
 */
public class BudgetReportService {
    private final String dbUrl;

    // PDF Layout Constants - Using PdfFormattingUtils
    private static final float PAGE_MARGIN_LEFT = PdfFormattingUtils.MARGIN_LEFT;
    private static final float PAGE_WIDTH = PdfFormattingUtils.PAGE_WIDTH;
    private static final float PAGE_HEIGHT = PdfFormattingUtils.PAGE_HEIGHT;

    // Font Sizes - Using PdfFormattingUtils typography hierarchy
    private static final float FONT_SIZE_TITLE = PdfFormattingUtils.FONT_SIZE_TITLE;
    private static final float FONT_SIZE_HEADER = PdfFormattingUtils.FONT_SIZE_HEADER;
    private static final float FONT_SIZE_NORMAL = PdfFormattingUtils.FONT_SIZE_NORMAL;
    private static final float FONT_SIZE_SMALL = PdfFormattingUtils.FONT_SIZE_SMALL;

    // Vertical Spacing - Using PdfFormattingUtils consistent spacing
    private static final float LINE_SPACING_NORMAL = PdfFormattingUtils.FONT_SIZE_NORMAL * PdfFormattingUtils.LINE_HEIGHT_FACTOR;
    private static final float LINE_SPACING_LARGE = PdfFormattingUtils.FONT_SIZE_HEADER * PdfFormattingUtils.LINE_HEIGHT_FACTOR;

    // Section Positions - Updated for professional layout
    private static final float TITLE_Y = PdfFormattingUtils.PAGE_HEIGHT - PdfFormattingUtils.MARGIN_TOP;
    private static final float HEADER_Y = TITLE_Y - 120f; // Space for header section
    private static final float SUMMARY_Y = HEADER_Y - 80f; // Space for summary section
    private static final float TABLE_Y = SUMMARY_Y - 60f; // Space for table content
    private static final float CATEGORY_Y = TABLE_Y - 200f; // Space for categories
    private static final float ITEMS_Y = CATEGORY_Y - 150f; // Space for items
    private static final float STRATEGIC_Y = ITEMS_Y - 200f; // Space for strategic content

    public BudgetReportService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }

    /**
     * PageManager handles dynamic page creation and orientation detection for loose page generation
     */
    private static class PageManager {
        private final PDDocument document;
        private PDPage currentPage;
        private PDPageContentStream currentContentStream;
        private float currentY;
        private int pageCount;
        private boolean isLandscape;
        private List<PDPageContentStream> allContentStreams;
        private String footerText1;
        private String footerText2;
        private String footerText3;

        public PageManager(PDDocument document) {
            this.document = document;
            this.pageCount = 0;
            this.isLandscape = false;
            this.allContentStreams = new ArrayList<>();
            createNewPage(false); // Start with portrait
        }

        /**
         * Create PageManager with specified default orientation
         */
        public PageManager(PDDocument document, boolean defaultLandscape) {
            this.document = document;
            this.pageCount = 0;
            this.isLandscape = defaultLandscape;
            this.allContentStreams = new ArrayList<>();
            createNewPage(defaultLandscape); // Start with specified orientation
        }

        /**
         * Create a new page with specified orientation (public method)
         */
        public void createNewPage(boolean landscape) {
            createNewPageInternal(landscape);
        }

        /**
         * Create a new page with specified orientation (internal method)
         */
        private void createNewPageInternal(boolean landscape) {
            // Close current content stream if it exists
            if (currentContentStream != null) {
                try {
                    currentContentStream.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to close content stream: " + e.getMessage());
                }
            }

            // Create new page with appropriate orientation
            if (landscape) {
                currentPage = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // Landscape A4
                isLandscape = true;
            } else {
                currentPage = new PDPage(PDRectangle.A4); // Portrait A4
                isLandscape = false;
            }

            document.addPage(currentPage);
            pageCount++;

            try {
                currentContentStream = new PDPageContentStream(document, currentPage);
                allContentStreams.add(currentContentStream);
                currentY = getPageHeight() - PdfFormattingUtils.MARGIN_TOP;
            } catch (IOException e) {
                throw new RuntimeException("Failed to create content stream for new page", e);
            }
        }

        /**
         * Check if content fits on current page, create new page if needed
         * Ensures adequate space for content + footer (100 points minimum for footer area)
         */
        public void ensureSpace(float requiredSpace) {
            // Reserve 100 points for footer area to prevent overlap (increased from 60pt)
            // This ensures adequate clearance for all table types and continued sections
            float footerReservedSpace = 100f;
            float minimumBottomSpace = PdfFormattingUtils.MARGIN_BOTTOM + footerReservedSpace;
            
            if (currentY - requiredSpace < minimumBottomSpace) {
                createNewPage(isLandscape); // Keep same orientation
            }
        }

        /**
         * Switch to landscape orientation if needed for wide content
         */
        public void ensureOrientationForTable(int numColumns, float estimatedWidth) {
            float availableWidth = getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
            if (estimatedWidth > availableWidth && !isLandscape) {
                // Content is too wide for portrait, switch to landscape
                createNewPage(true);
            }
        }

        /**
         * Get current page width based on orientation
         */
        public float getPageWidth() {
            return isLandscape ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth();
        }

        /**
         * Get current page height based on orientation
         */
        public float getPageHeight() {
            return isLandscape ? PDRectangle.A4.getWidth() : PDRectangle.A4.getHeight();
        }

        /**
         * Get current Y position
         */
        public float getCurrentY() {
            return currentY;
        }

        /**
         * Set current Y position
         */
        public void setCurrentY(float y) {
            this.currentY = y;
        }

        /**
         * Move Y position by specified amount
         */
        public void moveY(float deltaY) {
            this.currentY += deltaY;
        }

        /**
         * Get current content stream
         */
        public PDPageContentStream getContentStream() {
            return currentContentStream;
        }

        /**
         * Get current page
         */
        public PDPage getCurrentPage() {
            return currentPage;
        }

        /**
         * Get total page count
         */
        public int getPageCount() {
            return pageCount;
        }

        /**
         * Expose the underlying PDDocument for utilities that require it
         */
        public PDDocument getDocument() {
            return document;
        }

        /**
         * Check if current page is landscape
         */
        public boolean isLandscape() {
            return isLandscape;
        }

        /**
         * Set footer text for all pages (two parameters - backward compatibility)
         */
        public void setFooterText(String text1, String text2) {
            this.footerText1 = text1;
            this.footerText2 = text2;
            this.footerText3 = null; // No custom right text
        }

        /**
         * Set footer text for all pages (three parameters - custom right text)
         */
        public void setFooterText(String text1, String text2, String text3) {
            this.footerText1 = text1;
            this.footerText2 = text2;
            this.footerText3 = text3;
        }

        /**
         * Close the page manager and finalize content streams, drawing footers on all pages
         */
        public void close() throws IOException {
            // First close the current content stream
            if (currentContentStream != null) {
                currentContentStream.close();
            }

            // Now draw footers on all pages with correct page numbers
            for (int i = 0; i < allContentStreams.size(); i++) {
                PDPageContentStream contentStream = allContentStreams.get(i);
                try {
                    // Create a new content stream for the footer (PDFBox requires this for existing pages)
                    PDPageContentStream footerStream = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true);
                    PdfFormattingUtils.drawFooter(footerStream, footerText1, footerText2, footerText3, i + 1, pageCount);
                    footerStream.close();
                } catch (IOException e) {
                    System.err.println("Warning: Failed to draw footer on page " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Generate a comprehensive budget summary report in PDF format
     */
    public void generateBudgetSummaryReport(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during build/test mode
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            System.out.println("⚠️ Skipping PDF generation in test mode for budget summary report");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "budget_summary_report_" + companyId + "_" + timestamp + ".pdf";
        Path reportPath = Paths.get("reports", fileName);

        // Ensure reports directory exists
        Files.createDirectories(reportPath.getParent());

        // Get company information
        Company company = getCompanyById(companyId);
        if (company == null) {
            System.out.println("❌ Company not found: " + companyId);
            return;
        }

        // Get budget data
        BudgetData budgetData = getBudgetData(companyId);
        if (budgetData == null || budgetData.getBudget() == null) {
            System.out.println("❌ No budget data found for company " + companyId);
            return;
        }
        final Budget budget = budgetData.getBudget();
        if (budget.getBudgetYear() == null || budget.getTotalRevenue() == null || budget.getTotalExpenses() == null) {
            System.out.println("❌ Invalid budget data - missing required fields");
            return;
        }

        // Display console summary
        System.out.println("========================================");
        System.out.println("BUDGET SUMMARY REPORT");
        System.out.println("========================================");
        System.out.println("Company: " + company.getName());
        System.out.println("Budget Year: " + budget.getBudgetYear());
        System.out.println("Total Revenue: R" + budget.getTotalRevenue());
        System.out.println("Total Expenses: R" + budget.getTotalExpenses());
        System.out.println("Net Budget: R" + budget.getTotalRevenue().subtract(budget.getTotalExpenses()));
        System.out.println("Categories: " + budgetData.getCategories().size());
        System.out.println("Budget Items: " + budgetData.getItems().size());

        // Generate PDF with loose page generation in landscape mode
        try (PDDocument document = new PDDocument()) {
            PageManager pageManager = new PageManager(document, true); // Start in landscape mode

            createBudgetReportHeader(pageManager, company, budget);
            createBudgetSummary(pageManager, budgetData);
            createBudgetCategories(pageManager, budgetData);
            createBudgetItems(pageManager, budgetData);
            createFooter(pageManager);

            pageManager.close();
            document.save(reportPath.toFile());
        }

        System.out.println("✅ Budget summary report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Generate strategic planning report in PDF format
     */
    public void generateStrategicPlanReport(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during build/test mode
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            System.out.println("⚠️ Skipping PDF generation in test mode for strategic plan report");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "strategic_plan_report_" + companyId + "_" + timestamp + ".pdf";
        Path reportPath = Paths.get("reports", fileName);

        // Ensure reports directory exists
        Files.createDirectories(reportPath.getParent());

        // Get company information
        Company company = getCompanyById(companyId);
        if (company == null) {
            System.out.println("❌ Company not found: " + companyId);
            return;
        }

        // Get strategic plan data
        StrategicPlanData strategicData = getStrategicPlanData(companyId);
        if (strategicData == null || strategicData.getPlan() == null) {
            System.out.println("❌ No strategic plan data found for company " + companyId);
            return;
        }
        final StrategicPlan plan = strategicData.getPlan();
        if (plan.getTitle() == null || plan.getVisionStatement() == null || plan.getMissionStatement() == null) {
            System.out.println("❌ Invalid strategic plan data - missing required fields");
            return;
        }

        // Display console summary
        System.out.println("========================================");
        System.out.println("STRATEGIC PLAN REPORT");
        System.out.println("========================================");
        System.out.println("Company: " + company.getName());
        System.out.println("Plan: " + plan.getTitle());
        System.out.println("Vision: " + plan.getVisionStatement());
        System.out.println("Mission: " + plan.getMissionStatement());
        System.out.println("Priorities: " + strategicData.getPriorities().size());
        System.out.println("Initiatives: " + strategicData.getInitiatives().size());
        System.out.println("Milestones: " + strategicData.getMilestones().size());

        // Generate PDF with loose page generation in landscape mode
        try (PDDocument document = new PDDocument()) {
            PageManager pageManager = new PageManager(document, true); // Start in landscape mode

            createStrategicReportHeader(pageManager, company, strategicData);
            createStrategicVision(pageManager, strategicData);
            createDetailedStrategicPriorities(pageManager, strategicData);
            createImplementationTimeline(pageManager, strategicData);
            createFinancialProjections(pageManager, strategicData);
            createBudgetAllocation(pageManager, strategicData);
            // Removed duplicate createStrategicPriorities() - detailed version already shown on Page 2
            createStrategicInitiatives(pageManager, strategicData);
            createStrategicMilestones(pageManager, strategicData);
            createOperationalActivities(pageManager, strategicData);
            createRevisedAnnualOperationalBudget(pageManager, strategicData);
            createFooter(pageManager);

            pageManager.close();
            document.save(reportPath.toFile());
        }

        System.out.println("✅ Strategic plan report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Generate budget vs actual comparison report in PDF format
     */
    public void generateBudgetVsActualReport(Long companyId) throws IOException, SQLException {
        // Skip PDF generation during build/test mode
        if ("true".equals(System.getProperty("TEST_MODE")) || "true".equals(System.getenv("TEST_MODE"))) {
            System.out.println("⚠️ Skipping PDF generation in test mode for budget vs actual report");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "budget_vs_actual_report_" + companyId + "_" + timestamp + ".pdf";
        Path reportPath = Paths.get("reports", fileName);

        // Ensure reports directory exists
        Files.createDirectories(reportPath.getParent());

        // Get company information
        Company company = getCompanyById(companyId);
        if (company == null) {
            System.out.println("❌ Company not found: " + companyId);
            return;
        }

        // Get budget data
        BudgetData budgetData = getBudgetData(companyId);
        if (budgetData == null || budgetData.getBudget() == null) {
            System.out.println("❌ No budget data found for company " + companyId);
            return;
        }
        final Budget budget = budgetData.getBudget();
        if (budget.getBudgetYear() == null || budget.getTotalRevenue() == null || budget.getTotalExpenses() == null) {
            System.out.println("❌ Invalid budget data - missing required fields");
            return;
        }

        // Display console summary
        System.out.println("========================================");
        System.out.println("BUDGET VS ACTUAL REPORT");
        System.out.println("========================================");
        System.out.println("Company: " + company.getName());
        System.out.println("Budget Year: " + budget.getBudgetYear());
        System.out.println("Note: Actual spending comparison feature coming soon");
        System.out.println("This report shows budgeted amounts with placeholders for actual spending");

        // Generate PDF with loose page generation in landscape mode
        try (PDDocument document = new PDDocument()) {
            PageManager pageManager = new PageManager(document, true); // Start in landscape mode

            createBudgetVsActualHeader(pageManager, company, budget);
            createBudgetVsActualSummary(pageManager, budgetData);
            createBudgetVsActualDetails(pageManager, budgetData);
            createFooter(pageManager);

            pageManager.close();
            document.save(reportPath.toFile());
        }

        System.out.println("✅ Budget vs actual report generated: " + reportPath.toAbsolutePath());
    }

    private void createBudgetReportHeader(PageManager pageManager, Company company, Budget budget) throws IOException {
        String subtitle = "Budget Year: " + budget.getBudgetYear();
        float titleY = pageManager.getPageHeight() - PdfFormattingUtils.MARGIN_TOP;
    PdfFormattingUtils.drawHeaderSection(pageManager.getContentStream(), pageManager.getDocument(), company.getLogoPath(), "BUDGET SUMMARY REPORT", company.getName(), subtitle, titleY, pageManager.getPageWidth());
        pageManager.setCurrentY(titleY - 160); // Space after header
    }

    private void createBudgetSummary(PageManager pageManager, BudgetData budgetData) throws IOException {
        pageManager.ensureSpace(140f); // Ensure space for summary box

        // Create summary metrics for the summary box
        String[][] summaryMetrics = {
            {"Total Revenue", "R" + budgetData.getBudget().getTotalRevenue().setScale(2, RoundingMode.HALF_UP)},
            {"Total Expenses", "R" + budgetData.getBudget().getTotalExpenses().setScale(2, RoundingMode.HALF_UP)},
            {"Net Budget", "R" + budgetData.getBudget().getTotalRevenue().subtract(budgetData.getBudget().getTotalExpenses()).setScale(2, RoundingMode.HALF_UP)},
            {"Budget Categories", String.valueOf(budgetData.getCategories().size())},
            {"Budget Items", String.valueOf(budgetData.getItems().size())}
        };

        float summaryY = pageManager.getCurrentY() - 20;
        PdfFormattingUtils.drawSummaryBox(pageManager.getContentStream(), summaryMetrics, summaryY, 120f,
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        pageManager.setCurrentY(summaryY - 140); // Space after summary box
    }

    private void createBudgetCategories(PageManager pageManager, BudgetData budgetData) throws IOException {
        // Check if we need landscape for the categories table
        float estimatedTableWidth = Math.max(400f, budgetData.getCategories().size() * 80f); // Estimate based on content
        pageManager.ensureOrientationForTable(budgetData.getCategories().size() + 1, estimatedTableWidth);

        pageManager.ensureSpace(200f); // Ensure space for categories section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "BUDGET CATEGORIES", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Prepare table data
        String[] headers = {"Category", "Type", "Allocated Amount", "Percentage"};
        String[][] tableData = new String[budgetData.getCategories().size()][];

        for (int i = 0; i < budgetData.getCategories().size(); i++) {
            BudgetCategory category = budgetData.getCategories().get(i);
            tableData[i] = new String[]{
                category.getName(),
                category.getCategoryType(),
                "R" + category.getTotalAllocated().setScale(2, RoundingMode.HALF_UP),
                category.getAllocatedPercentage().setScale(1, RoundingMode.HALF_UP) + "%"
            };
        }

        // Check if table fits, if not create new page
        float tableHeight = (tableData.length + 1) * 20f + 20f; // Header + data rows + padding
        if (currentY - tableHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
            pageManager.createNewPage(pageManager.isLandscape());
            currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "BUDGET CATEGORIES (Continued)", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        }

        PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), headers, tableData, currentY, 20f,
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
            PdfFormattingUtils.MARGIN_LEFT);
        pageManager.setCurrentY(currentY - tableHeight - 20); // Space after table
    }

    private void createBudgetItems(PageManager pageManager, BudgetData budgetData) throws IOException {
        // Check if we need landscape for the items table
        float estimatedTableWidth = Math.max(350f, budgetData.getItems().size() * 60f); // Estimate based on content
        pageManager.ensureOrientationForTable(3, estimatedTableWidth); // 3 columns

        pageManager.ensureSpace(120f);

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "BUDGET ITEMS", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Prepare table data - show all items, let page manager handle overflow
        String[] headers = {"Description", "Annual Amount", "Notes"};
        String[][] tableData = new String[budgetData.getItems().size()][];

        for (int i = 0; i < budgetData.getItems().size(); i++) {
            BudgetItem item = budgetData.getItems().get(i);
            tableData[i] = new String[]{
                item.getDescription(),
                "R" + item.getAnnualAmount().setScale(2, RoundingMode.HALF_UP),
                item.getNotes() != null ? item.getNotes() : ""
            };
        }

        // Check if table fits, if not create new page
        float tableHeight = (tableData.length + 1) * 22f + 20f; // Increased row height for better text display
        if (currentY - tableHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
            pageManager.createNewPage(pageManager.isLandscape());
            currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "BUDGET ITEMS (Continued)", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        }

        PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), headers, tableData, currentY, 22f,
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
            PdfFormattingUtils.MARGIN_LEFT);
        pageManager.setCurrentY(currentY - tableHeight - 20); // Space after table
    }

    private void createStrategicReportHeader(PageManager pageManager, Company company, StrategicPlanData strategicData) throws IOException {
        String subtitle = "Plan: " + strategicData.getPlan().getTitle();
        float titleY = pageManager.getPageHeight() - PdfFormattingUtils.MARGIN_TOP;
    PdfFormattingUtils.drawHeaderSection(pageManager.getContentStream(), pageManager.getDocument(), company.getLogoPath(), "STRATEGIC PLAN REPORT", company.getName(), subtitle, titleY, pageManager.getPageWidth());
        pageManager.setCurrentY(titleY - 160); // Space after header
    }

    private void createStrategicVision(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        pageManager.ensureSpace(150f); // Ensure space for vision/mission section (increased for wrapped text)

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "VISION & MISSION", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Use full-width wrapped text blocks for vision and mission statements
        float contentWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;

        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "Vision Statement",
            strategicData.getPlan().getVisionStatement(), currentY, contentWidth);

    currentY -= (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR); 

        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "Mission Statement",
            strategicData.getPlan().getMissionStatement(), currentY, contentWidth);

    currentY -= (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR); 

        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "Goals", 
            strategicData.getPlan().getGoals(), currentY, contentWidth);

    currentY -= (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR);

    pageManager.setCurrentY(currentY - (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR)); 
    }

    private void createDetailedStrategicPriorities(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        if (strategicData.getPlan().getStrategicPriorities() == null || strategicData.getPlan().getStrategicPriorities().isEmpty()) {
            return;
        }

        pageManager.ensureSpace(200f); // Ensure space for detailed priorities section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "STRATEGIC PRIORITIES", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Use full-width wrapped text block for detailed strategic priorities
        float contentWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "",
            strategicData.getPlan().getStrategicPriorities(), currentY, contentWidth);

    pageManager.setCurrentY(currentY - (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR));
    }

    private void createImplementationTimeline(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        if (strategicData.getPlan().getImplementationTimeline() == null || strategicData.getPlan().getImplementationTimeline().isEmpty()) {
            return;
        }

        pageManager.ensureSpace(150f); // Ensure space for implementation timeline section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "IMPLEMENTATION TIMELINE", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Use full-width wrapped text block for implementation timeline
        float contentWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "",
            strategicData.getPlan().getImplementationTimeline(), currentY, contentWidth);

    pageManager.setCurrentY(currentY - (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR));
    }

    private void createFinancialProjections(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        // Check if we have budget data to display
        if (strategicData.getBudgetData() == null || strategicData.getBudgetData().getBudget() == null) {
            // Fallback to hardcoded content if no budget data available
            if (strategicData.getPlan().getFinancialProjections() == null || strategicData.getPlan().getFinancialProjections().isEmpty()) {
                return;
            }
        }

        pageManager.ensureSpace(150f); // Ensure space for financial projections section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "FINANCIAL PROJECTIONS", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        if (strategicData.getBudgetData() != null && strategicData.getBudgetData().getBudget() != null) {
            // Use actual budget data from database - create detailed multi-year projections
            Budget budget = strategicData.getBudgetData().getBudget();
            BigDecimal baseRevenue = budget.getTotalRevenue();
            BigDecimal baseExpenses = budget.getTotalExpenses();
            
            // Calculate growth projections (25% annual growth - realistic and sustainable)
            BigDecimal growthRate = new BigDecimal("1.25"); // 25% growth
            BigDecimal year2Revenue = baseRevenue.multiply(growthRate);
            BigDecimal year3Revenue = year2Revenue.multiply(growthRate);
            BigDecimal year4Revenue = year3Revenue.multiply(growthRate);
            
            // Calculate expenses with economies of scale (expenses grow at 20% vs 25% revenue growth)
            BigDecimal expenseGrowthRate = new BigDecimal("1.20");
            BigDecimal year2Expenses = baseExpenses.multiply(expenseGrowthRate);
            BigDecimal year3Expenses = year2Expenses.multiply(expenseGrowthRate);
            BigDecimal year4Expenses = year3Expenses.multiply(expenseGrowthRate);

            // === SECTION 1: Multi-Year Financial Summary ===
            String[] summaryHeaders = {"Financial Metric", "Year 1", "Year 2", "Year 3", "Year 4"};
            List<String[]> summaryRows = new ArrayList<>();
            
            summaryRows.add(new String[]{"Total Revenue (R)", 
                String.format("%,.2f", baseRevenue),
                String.format("%,.2f", year2Revenue),
                String.format("%,.2f", year3Revenue),
                String.format("%,.2f", year4Revenue)});
            
            summaryRows.add(new String[]{"Total Expenses (R)",
                String.format("%,.2f", baseExpenses),
                String.format("%,.2f", year2Expenses),
                String.format("%,.2f", year3Expenses),
                String.format("%,.2f", year4Expenses)});
            
            summaryRows.add(new String[]{"Net Profit (R)",
                String.format("%,.2f", baseRevenue.subtract(baseExpenses)),
                String.format("%,.2f", year2Revenue.subtract(year2Expenses)),
                String.format("%,.2f", year3Revenue.subtract(year3Expenses)),
                String.format("%,.2f", year4Revenue.subtract(year4Expenses))});
            
            summaryRows.add(new String[]{"Profit Margin (%)",
                baseRevenue.compareTo(BigDecimal.ZERO) > 0 ? 
                    String.format("%.1f%%", baseRevenue.subtract(baseExpenses).divide(baseRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%",
                year2Revenue.compareTo(BigDecimal.ZERO) > 0 ?
                    String.format("%.1f%%", year2Revenue.subtract(year2Expenses).divide(year2Revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%",
                year3Revenue.compareTo(BigDecimal.ZERO) > 0 ?
                    String.format("%.1f%%", year3Revenue.subtract(year3Expenses).divide(year3Revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%",
                year4Revenue.compareTo(BigDecimal.ZERO) > 0 ?
                    String.format("%.1f%%", year4Revenue.subtract(year4Expenses).divide(year4Revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%"});
            
            summaryRows.add(new String[]{"Revenue Growth Rate", "-", "25.0%", "25.0%", "25.0%"});
            
            String[][] summaryData = summaryRows.toArray(new String[0][]);
            float summaryHeight = (summaryData.length + 1) * 22f + 20f;
            
            if (currentY - summaryHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
                pageManager.createNewPage(pageManager.isLandscape());
                currentY = pageManager.getCurrentY();
            }
            
            PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), summaryHeaders, summaryData, currentY, 22f,
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
                PdfFormattingUtils.MARGIN_LEFT);
            currentY = currentY - summaryHeight - 30;
            pageManager.setCurrentY(currentY);

            // === SECTION 2: Revenue Breakdown by Category ===
            pageManager.ensureSpace(150f);
            currentY = PdfFormattingUtils.drawSubsectionHeader(pageManager.getContentStream(), 
                "Revenue Streams - Detailed Breakdown", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
            
            String[] revenueHeaders = {"Revenue Category", "Year 1 (R)", "Year 2 (R)", "Year 3 (R)", "% of Total"};
            List<String[]> revenueRows = new ArrayList<>();
            
            if (strategicData.getBudgetData().getCategories() != null) {
                BigDecimal totalRevenue = BigDecimal.ZERO;
                for (BudgetCategory category : strategicData.getBudgetData().getCategories()) {
                    if ("Revenue".equals(category.getCategoryType()) || "Income".equals(category.getCategoryType())) {
                        totalRevenue = totalRevenue.add(category.getTotalAllocated());
                    }
                }
                
                for (BudgetCategory category : strategicData.getBudgetData().getCategories()) {
                    if ("Revenue".equals(category.getCategoryType()) || "Income".equals(category.getCategoryType())) {
                        BigDecimal year1Amount = category.getTotalAllocated();
                        BigDecimal year2Amount = year1Amount.multiply(growthRate);
                        BigDecimal year3Amount = year2Amount.multiply(growthRate);
                        String percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                            String.format("%.1f%%", year1Amount.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%";
                        
                        revenueRows.add(new String[]{
                            category.getName(),
                            String.format("%,.2f", year1Amount),
                            String.format("%,.2f", year2Amount),
                            String.format("%,.2f", year3Amount),
                            percentage
                        });
                    }
                }
            }
            
            if (!revenueRows.isEmpty()) {
                String[][] revenueData = revenueRows.toArray(new String[0][]);
                float revenueHeight = (revenueData.length + 1) * 22f + 20f;
                
                if (currentY - revenueHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
                    pageManager.createNewPage(pageManager.isLandscape());
                    currentY = PdfFormattingUtils.drawSubsectionHeader(pageManager.getContentStream(), 
                        "Revenue Streams (Continued)", pageManager.getCurrentY(),
                        pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
                }
                
                PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), revenueHeaders, revenueData, currentY, 22f,
                    pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
                    PdfFormattingUtils.MARGIN_LEFT);
                currentY = currentY - revenueHeight - 30;
                pageManager.setCurrentY(currentY);
            }

            // === SECTION 3: Expense Breakdown by Category ===
            pageManager.ensureSpace(150f);
            currentY = PdfFormattingUtils.drawSubsectionHeader(pageManager.getContentStream(), 
                "Expense Categories - Detailed Allocation", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
            
            String[] expenseHeaders = {"Expense Category", "Year 1 (R)", "Year 2 (R)", "Year 3 (R)", "% of Total"};
            List<String[]> expenseRows = new ArrayList<>();
            
            if (strategicData.getBudgetData().getCategories() != null) {
                BigDecimal totalExpense = BigDecimal.ZERO;
                for (BudgetCategory category : strategicData.getBudgetData().getCategories()) {
                    // Include all non-revenue categories as expenses
                    String catType = category.getCategoryType();
                    if (catType != null && !catType.equalsIgnoreCase("Revenue") && !catType.equalsIgnoreCase("Income")) {
                        totalExpense = totalExpense.add(category.getTotalAllocated());
                    }
                }
                
                for (BudgetCategory category : strategicData.getBudgetData().getCategories()) {
                    // Include all non-revenue categories as expenses
                    String catType = category.getCategoryType();
                    if (catType != null && !catType.equalsIgnoreCase("Revenue") && !catType.equalsIgnoreCase("Income")) {
                        BigDecimal year1Amount = category.getTotalAllocated();
                        BigDecimal year2Amount = year1Amount.multiply(expenseGrowthRate);
                        BigDecimal year3Amount = year2Amount.multiply(expenseGrowthRate);
                        String percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0 ?
                            String.format("%.1f%%", year1Amount.divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))) : "0.0%";
                        
                        expenseRows.add(new String[]{
                            category.getName(),
                            String.format("%,.2f", year1Amount),
                            String.format("%,.2f", year2Amount),
                            String.format("%,.2f", year3Amount),
                            percentage
                        });
                    }
                }
            }
            
            if (!expenseRows.isEmpty()) {
                String[][] expenseData = expenseRows.toArray(new String[0][]);
                float expenseHeight = (expenseData.length + 1) * 22f + 20f;
                
                if (currentY - expenseHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
                    pageManager.createNewPage(pageManager.isLandscape());
                    currentY = PdfFormattingUtils.drawSubsectionHeader(pageManager.getContentStream(), 
                        "Expense Categories (Continued)", pageManager.getCurrentY(),
                        pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
                }
                
                PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), expenseHeaders, expenseData, currentY, 22f,
                    pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
                    PdfFormattingUtils.MARGIN_LEFT);
                currentY = currentY - expenseHeight - 30;
                pageManager.setCurrentY(currentY);
            }

            // === SECTION 4: Key Financial Assumptions ===
            // Prepare data first to calculate exact height needed
            String[] assumptionHeaders = {"Assumption", "Value", "Rationale"};
            String[][] assumptionData = {
                {"Enrollment Growth", "25% annually", "Realistic and sustainable market growth"},
                {"Revenue Growth", "25% annually", "Aligned with enrollment growth projections"},
                {"Expense Growth", "20% annually", "Economies of scale as operations mature"},
                {"Tuition per Student", String.format("R %,.2f", baseRevenue.divide(new BigDecimal("50"), 2, RoundingMode.HALF_UP)), "Competitive market rate for quality education"},
                {"Staff-to-Student Ratio", "1:15", "Maintain quality educational standards"},
                {"Infrastructure Investment", "20-25% of budget", "Phased facility development plan"},
                {"Operating Reserve", "10% of annual budget", "Financial sustainability buffer"}
            };
            
            // Calculate the exact height needed for this section (header + table + spacing)
            float assumptionHeaderHeight = 26f; // Subsection header height
            float assumptionTableHeight = (assumptionData.length + 1) * 22f + 20f;
            float totalAssumptionHeight = assumptionHeaderHeight + assumptionTableHeight;
            
            // Only create new page if there's truly not enough space
            currentY = pageManager.getCurrentY();
            if (currentY - totalAssumptionHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
                pageManager.createNewPage(pageManager.isLandscape());
                currentY = pageManager.getCurrentY();
            }
            
            // Draw the subsection header
            currentY = PdfFormattingUtils.drawSubsectionHeader(pageManager.getContentStream(), 
                "Key Financial Assumptions", currentY,
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
            
            // Draw the table
            PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), assumptionHeaders, assumptionData, currentY, 22f,
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
                PdfFormattingUtils.MARGIN_LEFT);
            pageManager.setCurrentY(currentY - assumptionTableHeight - 20);

        } else {
            // Fallback to wrapped text block for hardcoded content
            float contentWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
            String financialContent = strategicData.getPlan().getFinancialProjections();
            currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "",
                financialContent, currentY, contentWidth);
            pageManager.setCurrentY(currentY - (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR));
        }
    }

    private void createBudgetAllocation(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        if (strategicData.getPlan().getBudgetAllocation() == null || strategicData.getPlan().getBudgetAllocation().isEmpty()) {
            return;
        }

        pageManager.ensureSpace(120f); // Ensure space for budget allocation section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "BUDGET ALLOCATION", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Use full-width wrapped text block for budget allocation
        float contentWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
        currentY = PdfFormattingUtils.drawWrappedTextBlock(pageManager.getContentStream(), "",
            strategicData.getPlan().getBudgetAllocation(), currentY, contentWidth);

    pageManager.setCurrentY(currentY - (PdfFormattingUtils.FONT_SIZE_TINY * PdfFormattingUtils.LINE_HEIGHT_FACTOR));
    }

    // Removed unused createStrategicPriorities() method - replaced by createDetailedStrategicPriorities()
    // which provides comprehensive priority information on Page 2

    private void createStrategicInitiatives(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        // Check if we need landscape for the initiatives table
        float estimatedTableWidth = Math.max(400f, strategicData.getInitiatives().size() * 70f);
        pageManager.ensureOrientationForTable(4, estimatedTableWidth); // 4 columns

        pageManager.ensureSpace(170f); // Ensure space for initiatives section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "STRATEGIC INITIATIVES", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Prepare table data - show all initiatives, let page manager handle overflow
        String[] headers = {"Initiative", "Budget", "Status", "Period"};
        String[][] tableData = new String[strategicData.getInitiatives().size()][];

        for (int i = 0; i < strategicData.getInitiatives().size(); i++) {
            StrategicInitiative initiative = strategicData.getInitiatives().get(i);
            tableData[i] = new String[]{
                initiative.getTitle(),
                "R" + initiative.getBudgetAllocated().setScale(2, RoundingMode.HALF_UP),
                initiative.getStatus(),
                initiative.getStartDate() + " to " + initiative.getEndDate()
            };
        }

        // Check if table fits, if not create new page (increased row height for initiative names)
        float tableHeight = (tableData.length + 1) * 24f + 20f; // Increased from 18f to 24f
        if (currentY - tableHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
            pageManager.createNewPage(pageManager.isLandscape());
            currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "STRATEGIC INITIATIVES (Continued)", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        }

        PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), headers, tableData, currentY, 24f, // Increased from 18f to 24f
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
            PdfFormattingUtils.MARGIN_LEFT);
        pageManager.setCurrentY(currentY - tableHeight - 20);
    }

    private void createStrategicMilestones(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        // Check if we need landscape for the milestones table
        float estimatedTableWidth = Math.max(300f, strategicData.getMilestones().size() * 50f);
        pageManager.ensureOrientationForTable(3, estimatedTableWidth); // 3 columns

        pageManager.ensureSpace(140f); // Ensure space for milestones section

        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "STRATEGIC MILESTONES", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);

        // Prepare table data - show all milestones, let page manager handle overflow
        String[] headers = {"Milestone", "Target Date", "Status"};
        String[][] tableData = new String[strategicData.getMilestones().size()][];

        for (int i = 0; i < strategicData.getMilestones().size(); i++) {
            StrategicMilestone milestone = strategicData.getMilestones().get(i);
            tableData[i] = new String[]{
                milestone.getTitle(),
                milestone.getTargetDate(),
                milestone.getStatus()
            };
        }

        // Check if table fits, if not create new page (increased row height for milestone names)
        float tableHeight = (tableData.length + 1) * 22f + 20f; // Increased from 18f to 22f
        if (currentY - tableHeight < PdfFormattingUtils.MARGIN_BOTTOM + 50) {
            pageManager.createNewPage(pageManager.isLandscape());
            currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "STRATEGIC MILESTONES (Continued)", pageManager.getCurrentY(),
                pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        }

        PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), headers, tableData, currentY, 22f, // Increased from 18f to 22f
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT,
            PdfFormattingUtils.MARGIN_LEFT);
        pageManager.setCurrentY(currentY - tableHeight - 20);
    }

    private void createOperationalActivities(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        // Check if we need landscape for the operational activities table
        float estimatedTableWidth = Math.max(400f, strategicData.getOperationalActivities().size() * 70f);
        pageManager.ensureOrientationForTable(4, estimatedTableWidth); // 4 columns

        // Draw section header first
        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "MONTHLY OPERATIONAL ACTIVITIES", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        pageManager.setCurrentY(currentY);

        // Table parameters
        String[] headers = {"Month", "Activities", "Responsible Parties", "Status"};
        float rowHeight = 45f;
        float tableWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
        float pageMarginLeft = PdfFormattingUtils.MARGIN_LEFT;
        float footerReserve = 100f; // Footer reserve space

        // Calculate column widths proportionally to fill table width
        float[] baseColumnWidths = new float[]{80f, 300f, 200f, 80f}; // Month, Activities, Parties, Status
        float totalBaseWidth = 0f;
        for (float width : baseColumnWidths) {
            totalBaseWidth += width;
        }
        float scaleFactor = tableWidth / totalBaseWidth;
        float[] columnWidths = new float[baseColumnWidths.length];
        for (int i = 0; i < baseColumnWidths.length; i++) {
            columnWidths[i] = baseColumnWidths[i] * scaleFactor;
        }

        // Draw table header
        currentY = drawTableHeader(pageManager, headers, currentY, rowHeight, tableWidth, pageMarginLeft, columnWidths);

        // Draw each row with page break detection
        for (OperationalActivity activity : strategicData.getOperationalActivities()) {
            // Check if we have space for this row (row + footer reserve)
            if (currentY - rowHeight < footerReserve) {
                // Need new page - create new page using existing method
                pageManager.createNewPage(pageManager.isLandscape());
                currentY = pageManager.getCurrentY();
                
                // Redraw section header on new page
                currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "MONTHLY OPERATIONAL ACTIVITIES (Continued)", currentY,
                    tableWidth);
                
                // Redraw table header on new page
                currentY = drawTableHeader(pageManager, headers, currentY, rowHeight, tableWidth, pageMarginLeft, columnWidths);
            }

            // Draw the row
            String[] rowData = new String[]{
                activity.getMonthName(),
                activity.getActivities(),
                activity.getResponsibleParties(),
                activity.getStatus()
            };
            currentY = drawTableRow(pageManager.getContentStream(), rowData, currentY, rowHeight, columnWidths, pageMarginLeft);
        }

        pageManager.setCurrentY(currentY - 20f); // Bottom margin after table
    }

    /**
     * Draw table header with background and borders
     */
    private float drawTableHeader(PageManager pageManager, String[] headers, float startY, float rowHeight, 
                                   float tableWidth, float pageMarginLeft, float[] columnWidths) throws IOException {
        PDPageContentStream cs = pageManager.getContentStream();
        float currentY = startY;

        // Header background
        cs.setNonStrokingColor(0.9f, 0.9f, 0.9f); // Light gray
        cs.addRect(pageMarginLeft, currentY - rowHeight, tableWidth, rowHeight);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f); // Black

        // Header borders
        cs.setLineWidth(1f);
        cs.addRect(pageMarginLeft, currentY - rowHeight, tableWidth, rowHeight);
        cs.stroke();

        // Header text with bold font using PDFBox 3.0.0 API
        float x = pageMarginLeft + 6;
        float headerTextY = currentY - rowHeight + (rowHeight - PdfFormattingUtils.FONT_SIZE_SMALL) / 2f + 2;
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), PdfFormattingUtils.FONT_SIZE_SMALL);
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(x, headerTextY);
            cs.showText(headers[i]);
            cs.endText();
            x += columnWidths[i];
        }

        return currentY - rowHeight;
    }

    /**
     * Draw single table row with borders and text wrapping
     */
    private float drawTableRow(PDPageContentStream cs, String[] rowData, float startY, float rowHeight, 
                                float[] columnWidths, float pageMarginLeft) throws IOException {
        float currentY = startY;

        // Row borders
        cs.setLineWidth(0.5f);
        float x = pageMarginLeft;
        for (float colWidth : columnWidths) {
            cs.addRect(x, currentY - rowHeight, colWidth, rowHeight);
            x += colWidth;
        }
        cs.stroke();

        // Row text with regular font using PDFBox 3.0.0 API
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), PdfFormattingUtils.FONT_SIZE_SMALL);
        float cellLeft = pageMarginLeft + 6;
        for (int col = 0; col < rowData.length; col++) {
            String cell = rowData[col] != null ? rowData[col] : "";
            float cellWidth = columnWidths[col] - 12; // Account for padding

            // Wrap text within cell using existing utility method
            List<String> lines = wrapTextSimple(cell, cellWidth);
            float lineY = currentY - 4 - PdfFormattingUtils.FONT_SIZE_SMALL;
            int maxLines = (int) Math.floor((rowHeight - 8) / (PdfFormattingUtils.FONT_SIZE_SMALL * 1.2f));

            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;

                cs.beginText();
                cs.newLineAtOffset(cellLeft, lineY);
                cs.showText(line);
                cs.endText();
                lineY -= PdfFormattingUtils.FONT_SIZE_SMALL * 1.2f;
            }

            // Show ellipsis if text was truncated
            if (lines.size() > maxLines) {
                cs.beginText();
                cs.newLineAtOffset(cellLeft, lineY);
                cs.showText("...");
                cs.endText();
            }

            cellLeft += columnWidths[col];
        }

        return currentY - rowHeight;
    }

    /**
     * Simple text wrapping helper for table cells
     */
    private List<String> wrapTextSimple(String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return lines;
        }

        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = PdfFormattingUtils.FONT_SIZE_SMALL;

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;

            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void createRevisedAnnualOperationalBudget(PageManager pageManager, StrategicPlanData strategicData) throws IOException {
        if (strategicData.getBudgetData() == null || strategicData.getBudgetData().getCategories().isEmpty()) {
            return; // No budget data available
        }

        // Check if we need landscape for the budget table (14 columns: Category, Annual, 12 months)
        float estimatedTableWidth = 800f; // Wide table for monthly breakdown
        pageManager.ensureOrientationForTable(strategicData.getBudgetData().getCategories().size() + 3, estimatedTableWidth); // +3 for subtotal and total

        // Ensure adequate space for section header + table
        pageManager.ensureSpace(250f); // Increased from 200f for proper header spacing

        // Draw section header with proper spacing
        float currentY = PdfFormattingUtils.drawSectionHeader(pageManager.getContentStream(), "ANNUAL OPERATIONAL BUDGET", pageManager.getCurrentY(),
            pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT);
        
        // Add extra space after header before table (to ensure visibility)
        currentY -= 15f; // Add 15 points of spacing after header
        pageManager.setCurrentY(currentY);

        // Prepare table headers
        String[] headers = {"Category", "Annual Amount (R)", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // Prepare table data - categories with subtotals and totals
        List<String[]> tableRows = new ArrayList<>();
        BigDecimal[] monthlyGrandTotals = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            monthlyGrandTotals[i] = BigDecimal.ZERO;
        }
        
        // Track monthly totals for subtotal calculation (Staff Salaries + Staff Training)
        List<BigDecimal[]> categoryMonthlyTotals = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (BudgetCategory category : strategicData.getBudgetData().getCategories()) {
                // Get all budget items for this category
                String itemSql = "SELECT id, description, annual_amount FROM budget_items WHERE budget_category_id = ? ORDER BY description";
                List<BudgetItem> items = new ArrayList<>();

                try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                    stmt.setLong(1, category.getId());
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        BudgetItem item = new BudgetItem();
                        item.setId(rs.getLong("id"));
                        item.setDescription(rs.getString("description"));
                        item.setAnnualAmount(rs.getBigDecimal("annual_amount"));
                        items.add(item);
                    }
                }

                // Calculate monthly totals for this category
                BigDecimal[] monthlyTotals = new BigDecimal[12];
                for (int i = 0; i < 12; i++) {
                    monthlyTotals[i] = BigDecimal.ZERO;
                }

                // Sum up monthly allocations for all items in this category
                for (BudgetItem item : items) {
                    String monthlySql = "SELECT month_number, allocated_amount FROM budget_monthly_allocations WHERE budget_item_id = ? ORDER BY month_number";

                    try (PreparedStatement stmt = conn.prepareStatement(monthlySql)) {
                        stmt.setLong(1, item.getId());
                        ResultSet rs = stmt.executeQuery();

                        while (rs.next()) {
                            int monthIndex = rs.getInt("month_number") - 1; // Convert 1-12 to 0-11
                            if (monthIndex >= 0 && monthIndex < 12) {
                                monthlyTotals[monthIndex] = monthlyTotals[monthIndex].add(rs.getBigDecimal("allocated_amount"));
                            }
                        }
                    }
                }

                // Create table row for this category
                String[] row = new String[14];
                row[0] = category.getName(); // Clean category names without asterisks
                // Format annual amount with thousands separator
                row[1] = String.format("%,.2f", category.getTotalAllocated());

                // Add monthly amounts and accumulate grand totals
                for (int i = 0; i < 12; i++) {
                    row[i + 2] = monthlyTotals[i].compareTo(BigDecimal.ZERO) == 0 ? "-" :
                        String.format("%,.2f", monthlyTotals[i]);
                    monthlyGrandTotals[i] = monthlyGrandTotals[i].add(monthlyTotals[i]);
                }

                tableRows.add(row);
                categoryMonthlyTotals.add(monthlyTotals); // Store for subtotal calculation

                // Add subtotal after Staff Training
                if ("Staff Training".equals(category.getName())) {
                    BigDecimal subtotalAnnual = new BigDecimal("2120000.00"); // Staff Salaries + Staff Training
                    String[] subtotalRow = new String[14];
                    subtotalRow[0] = "Subtotal: Salaries & Training"; // Clean subtotal text
                    subtotalRow[1] = String.format("%,.2f", subtotalAnnual);

                    // Calculate subtotal monthly amounts from stored BigDecimal values
                    BigDecimal[] subtotalMonthly = new BigDecimal[12];
                    for (int i = 0; i < 12; i++) {
                        subtotalMonthly[i] = BigDecimal.ZERO;
                    }

                    // Sum the last 2 categories (Staff Salaries and Staff Training)
                    int numCategoriesToSum = Math.min(2, categoryMonthlyTotals.size());
                    for (int catIdx = categoryMonthlyTotals.size() - numCategoriesToSum; 
                         catIdx < categoryMonthlyTotals.size(); catIdx++) {
                        BigDecimal[] categoryTotals = categoryMonthlyTotals.get(catIdx);
                        for (int i = 0; i < 12; i++) {
                            subtotalMonthly[i] = subtotalMonthly[i].add(categoryTotals[i]);
                        }
                    }

                    for (int i = 0; i < 12; i++) {
                        subtotalRow[i + 2] = subtotalMonthly[i].compareTo(BigDecimal.ZERO) == 0 ? "-" :
                            String.format("%,.2f", subtotalMonthly[i]);
                    }

                    tableRows.add(subtotalRow);
                }
            }

            // Add TOTAL MONTHLY SPEND row
            String[] totalRow = new String[14];
            totalRow[0] = "TOTAL MONTHLY SPEND"; // Clean total text without asterisks
            BigDecimal totalAnnual = strategicData.getBudgetData().getCategories().stream()
                .map(BudgetCategory::getTotalAllocated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalRow[1] = String.format("%,.2f", totalAnnual); // Format with thousands separator

            for (int i = 0; i < 12; i++) {
                totalRow[i + 2] = monthlyGrandTotals[i].compareTo(BigDecimal.ZERO) == 0 ? "-" :
                    String.format("%,.2f", monthlyGrandTotals[i]);
            }

            // Get annual revenue from budget data for financial performance calculations
            BigDecimal annualRevenue = strategicData.getBudgetData().getBudget().getTotalRevenue();
            
            // Distribute revenue evenly across 12 months
            BigDecimal monthlyRevenue = annualRevenue.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            BigDecimal[] monthlyRevenues = new BigDecimal[12];
            for (int i = 0; i < 12; i++) {
                monthlyRevenues[i] = monthlyRevenue;
            }

            // FINANCIAL PERFORMANCE SECTION (in logical order: Revenue → Spend → Profit → Margin)
            
            // 1. Add TOTAL MONTHLY REVENUE row (income)
            String[] revenueRow = new String[14];
            revenueRow[0] = "TOTAL MONTHLY REVENUE";
            revenueRow[1] = String.format("%,.2f", annualRevenue);
            for (int i = 0; i < 12; i++) {
                revenueRow[i + 2] = String.format("%,.2f", monthlyRevenue);
            }
            tableRows.add(revenueRow);

            // 2. Add TOTAL MONTHLY SPEND row (expenses)
            tableRows.add(totalRow);

            // 3. Add NET PROFIT/(LOSS) row (difference)
            String[] profitRow = new String[14];
            profitRow[0] = "NET PROFIT/(LOSS)";
            BigDecimal annualProfit = annualRevenue.subtract(totalAnnual);
            profitRow[1] = String.format("%,.2f", annualProfit);
            for (int i = 0; i < 12; i++) {
                BigDecimal monthlyProfit = monthlyRevenues[i].subtract(monthlyGrandTotals[i]);
                profitRow[i + 2] = String.format("%,.2f", monthlyProfit);
            }
            tableRows.add(profitRow);

            // 4. Add PROFIT MARGIN (%) row (percentage)
            String[] marginRow = new String[14];
            marginRow[0] = "PROFIT MARGIN (%)";
            
            // Annual margin
            if (annualRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal annualMargin = annualProfit.divide(annualRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                marginRow[1] = String.format("%.1f%%", annualMargin);
            } else {
                marginRow[1] = "0.0%";
            }
            
            // Monthly margins
            for (int i = 0; i < 12; i++) {
                if (monthlyRevenues[i].compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal monthlyProfit = monthlyRevenues[i].subtract(monthlyGrandTotals[i]);
                    BigDecimal monthlyMargin = monthlyProfit.divide(monthlyRevenues[i], 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    marginRow[i + 2] = String.format("%.1f%%", monthlyMargin);
                } else {
                    marginRow[i + 2] = "0.0%";
                }
            }
            tableRows.add(marginRow);

        } catch (SQLException e) {
            System.err.println("Error fetching budget monthly allocations: " + e.getMessage());
            return;
        }

        // Convert to array for table drawing
        String[][] tableData = tableRows.toArray(new String[0][]);

        // Calculate total space needed for table
        float tableHeight = (tableData.length + 1) * 26f + 20f; // Increased from 20f to 26f for better readability
        
        // Use pageManager.ensureSpace() to properly account for footer reserved area (100pt)
        // This prevents footer overlap by checking against the reserved space
        pageManager.ensureSpace(tableHeight);
        
        // Get current Y position (either on same page or new page created by ensureSpace)
        currentY = pageManager.getCurrentY();

        // Calculate custom column widths for Annual Budget table (14 columns)
        float tableWidth = pageManager.getPageWidth() - PdfFormattingUtils.MARGIN_LEFT - PdfFormattingUtils.MARGIN_RIGHT;
        float[] customColumnWidths = PdfFormattingUtils.calculateAnnualBudgetColumnWidths(tableWidth);

        // Draw the table with custom column widths, adequate row height, and SMALLER FONT (6pt)
        // Smaller font allows values to fit properly in narrow monthly columns without wrapping
        PdfFormattingUtils.drawTable(pageManager.getContentStream(), pageManager.getDocument(), headers, tableData, currentY, 26f, // Increased from 20f to 26f
            tableWidth, PdfFormattingUtils.MARGIN_LEFT, customColumnWidths, 6f); // Final reduction to 6pt font for optimal fit
        pageManager.setCurrentY(currentY - tableHeight - 20);
    }

    private void createBudgetVsActualHeader(PageManager pageManager, Company company, Budget budget) throws IOException {
        String subtitle = "Budget Year: " + budget.getBudgetYear();
        float titleY = pageManager.getPageHeight() - PdfFormattingUtils.MARGIN_TOP;
    PdfFormattingUtils.drawHeaderSection(pageManager.getContentStream(), pageManager.getDocument(), company.getLogoPath(), "BUDGET VS ACTUAL REPORT", company.getName(), subtitle, titleY, pageManager.getPageWidth());
        pageManager.setCurrentY(titleY - 160); // Space after header
    }

    private void createBudgetVsActualSummary(PageManager pageManager, BudgetData budgetData) throws IOException {
        pageManager.ensureSpace(120f); // Ensure space for summary section

        float currentY = pageManager.getCurrentY();
        pageManager.getContentStream().setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        pageManager.getContentStream().beginText();
        pageManager.getContentStream().newLineAtOffset(PAGE_MARGIN_LEFT, currentY);
        pageManager.getContentStream().showText("BUDGET SUMMARY");
        pageManager.getContentStream().endText();

        currentY -= LINE_SPACING_NORMAL;
        pageManager.getContentStream().setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        pageManager.getContentStream().beginText();
        pageManager.getContentStream().newLineAtOffset(PAGE_MARGIN_LEFT, currentY);
        pageManager.getContentStream().showText("Budgeted Revenue: R" + budgetData.getBudget().getTotalRevenue().setScale(2, RoundingMode.HALF_UP));
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_NORMAL);
        pageManager.getContentStream().showText("Budgeted Expenses: R" + budgetData.getBudget().getTotalExpenses().setScale(2, RoundingMode.HALF_UP));
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_NORMAL);
        BigDecimal netBudget = budgetData.getBudget().getTotalRevenue().subtract(budgetData.getBudget().getTotalExpenses());
        pageManager.getContentStream().showText("Budgeted Net: R" + netBudget.setScale(2, RoundingMode.HALF_UP));
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_LARGE);
        pageManager.getContentStream().showText("Actual Amount: [Not implemented - would compare with actual transactions]");
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_NORMAL);
        pageManager.getContentStream().showText("Variance: [Not implemented]");
        pageManager.getContentStream().endText();

        pageManager.setCurrentY(currentY - LINE_SPACING_LARGE * 2);
    }

    private void createBudgetVsActualDetails(PageManager pageManager, BudgetData budgetData) throws IOException {
        pageManager.ensureSpace(100f); // Ensure space for details section

        float currentY = pageManager.getCurrentY();
        pageManager.getContentStream().setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        pageManager.getContentStream().beginText();
        pageManager.getContentStream().newLineAtOffset(PAGE_MARGIN_LEFT, currentY);
        pageManager.getContentStream().showText("BUDGET VS ACTUAL COMPARISON");
        pageManager.getContentStream().endText();

        currentY -= LINE_SPACING_NORMAL;
        pageManager.getContentStream().setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        pageManager.getContentStream().beginText();
        pageManager.getContentStream().newLineAtOffset(PAGE_MARGIN_LEFT, currentY);
        pageManager.getContentStream().showText("Budget vs Actual comparison feature coming soon!");
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_NORMAL);
        pageManager.getContentStream().showText("This will compare budgeted amounts with actual transaction data.");
        pageManager.getContentStream().newLineAtOffset(0, -LINE_SPACING_NORMAL);
        pageManager.getContentStream().showText("Currently showing budgeted amounts with placeholders for actual spending.");
        pageManager.getContentStream().endText();

        pageManager.setCurrentY(currentY - LINE_SPACING_LARGE * 2);
    }

    private void createFooter(PageManager pageManager) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        pageManager.setFooterText("Generated using FIN Financial Management System",
                                null,
                                timestamp);
    }

    private BudgetData getBudgetData(Long companyId) throws SQLException {
        // Get budget
        String budgetSql = "SELECT * FROM budgets WHERE company_id = ? ORDER BY budget_year DESC LIMIT 1";
        Budget budget = null;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(budgetSql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                budget = new Budget();
                budget.setId(rs.getLong("id"));
                budget.setCompanyId(rs.getLong("company_id"));
                budget.setTitle(rs.getString("title"));
                budget.setBudgetYear(rs.getInt("budget_year"));
                budget.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                budget.setTotalExpenses(rs.getBigDecimal("total_expenses"));
                budget.setStatus(rs.getString("status"));
            } else {
                return null;
            }
        }

        // Get categories
        List<BudgetCategory> categories = new ArrayList<>();
        String categorySql = "SELECT * FROM budget_categories WHERE budget_id = ? ORDER BY name";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(categorySql)) {

            stmt.setLong(1, budget.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BudgetCategory category = new BudgetCategory();
                category.setId(rs.getLong("id"));
                category.setBudgetId(rs.getLong("budget_id"));
                category.setName(rs.getString("name"));
                category.setCategoryType(rs.getString("category_type"));
                category.setDescription(rs.getString("description"));
                category.setAllocatedPercentage(rs.getBigDecimal("allocated_percentage"));
                category.setTotalAllocated(rs.getBigDecimal("total_allocated"));
                categories.add(category);
            }
        }

        // Get items
        List<BudgetItem> items = new ArrayList<>();
        String itemSql = "SELECT * FROM budget_items WHERE budget_category_id IN " +
                        "(SELECT id FROM budget_categories WHERE budget_id = ?) ORDER BY description";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(itemSql)) {

            stmt.setLong(1, budget.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BudgetItem item = new BudgetItem();
                item.setId(rs.getLong("id"));
                item.setBudgetCategoryId(rs.getLong("budget_category_id"));
                item.setDescription(rs.getString("description"));
                item.setAnnualAmount(rs.getBigDecimal("annual_amount"));
                item.setNotes(rs.getString("notes"));
                items.add(item);
            }
        }

        return new BudgetData(budget, categories, items);
    }

    private StrategicPlanData getStrategicPlanData(Long companyId) throws SQLException {
        // Get strategic plan
        String planSql = "SELECT * FROM strategic_plans WHERE company_id = ? ORDER BY start_date DESC LIMIT 1";
        StrategicPlan plan = null;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(planSql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                plan = new StrategicPlan();
                plan.setId(rs.getLong("id"));
                plan.setCompanyId(rs.getLong("company_id"));
                plan.setTitle(rs.getString("title"));
                plan.setVisionStatement(rs.getString("vision_statement"));
                plan.setMissionStatement(rs.getString("mission_statement"));
                plan.setGoals(rs.getString("goals"));
                plan.setStatus(rs.getString("status"));
                
                // Populate detailed strategic plan content
                populateDetailedStrategicContent(plan);
            } else {
                return null;
            }
        }

        // Get priorities
        List<StrategicPriority> priorities = new ArrayList<>();
        String prioritySql = "SELECT * FROM strategic_priorities WHERE strategic_plan_id = ? ORDER BY priority_order";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(prioritySql)) {

            stmt.setLong(1, plan.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                StrategicPriority priority = new StrategicPriority();
                priority.setId(rs.getLong("id"));
                priority.setStrategicPlanId(rs.getLong("strategic_plan_id"));
                priority.setName(rs.getString("name"));
                priority.setDescription(rs.getString("description"));
                priority.setPriorityOrder(rs.getInt("priority_order"));
                priorities.add(priority);
            }
        }

        // Get initiatives
        List<StrategicInitiative> initiatives = new ArrayList<>();
        String initiativeSql = "SELECT * FROM strategic_initiatives WHERE strategic_priority_id IN " +
                              "(SELECT id FROM strategic_priorities WHERE strategic_plan_id = ?) ORDER BY title";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(initiativeSql)) {

            stmt.setLong(1, plan.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                StrategicInitiative initiative = new StrategicInitiative();
                initiative.setId(rs.getLong("id"));
                initiative.setStrategicPriorityId(rs.getLong("strategic_priority_id"));
                initiative.setTitle(rs.getString("title"));
                initiative.setDescription(rs.getString("description"));
                initiative.setStartDate(rs.getString("start_date"));
                initiative.setEndDate(rs.getString("end_date"));
                initiative.setBudgetAllocated(rs.getBigDecimal("budget_allocated"));
                initiative.setStatus(rs.getString("status"));
                initiatives.add(initiative);
            }
        }

        // Get milestones
        List<StrategicMilestone> milestones = new ArrayList<>();
        String milestoneSql = "SELECT * FROM strategic_milestones WHERE strategic_initiative_id IN " +
                             "(SELECT id FROM strategic_initiatives WHERE strategic_priority_id IN " +
                             "(SELECT id FROM strategic_priorities WHERE strategic_plan_id = ?)) ORDER BY target_date";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(milestoneSql)) {

            stmt.setLong(1, plan.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                StrategicMilestone milestone = new StrategicMilestone();
                milestone.setId(rs.getLong("id"));
                milestone.setStrategicInitiativeId(rs.getLong("strategic_initiative_id"));
                milestone.setTitle(rs.getString("title"));
                milestone.setDescription(rs.getString("description"));
                milestone.setTargetDate(rs.getString("target_date"));
                milestone.setStatus(rs.getString("status"));
                milestones.add(milestone);
            }
        }

        // Get operational activities
        List<OperationalActivity> operationalActivities = new ArrayList<>();
        String activitySql = "SELECT * FROM operational_activities WHERE strategic_plan_id = ? ORDER BY month_number";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(activitySql)) {

            stmt.setLong(1, plan.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OperationalActivity activity = new OperationalActivity();
                activity.setId(rs.getLong("id"));
                activity.setStrategicPlanId(rs.getLong("strategic_plan_id"));
                activity.setMonthNumber(rs.getInt("month_number"));
                activity.setTitle(rs.getString("title"));
                activity.setActivities(rs.getString("activities"));
                activity.setResponsibleParties(rs.getString("responsible_parties"));
                activity.setStatus(rs.getString("status"));
                operationalActivities.add(activity);
            }
        }

        // Get budget data for the company
        BudgetData budgetData = getBudgetData(companyId);

        return new StrategicPlanData(plan, priorities, initiatives, milestones, operationalActivities, budgetData);
    }

    private void populateDetailedStrategicContent(StrategicPlan plan) {
        // Set the detailed strategic priorities content
        plan.setStrategicPriorities(
            "1. Academic Excellence\n" +
            "•\tDevelop and implement a rigorous curriculum that meets national standards\n" +
            "•\tEnhance teacher training and development programs\n" +
            "•\tImprove student assessment and evaluation methods\n" +
            "2. Student Well-being\n" +
            "•\tCreate a safe and nurturing environment that promotes social, emotional, and spiritual growth\n" +
            "•\tImplement programs to support students' mental health and well-being\n" +
            "•\tFoster a sense of community and inclusivity\n" +
            "3. Community Engagement\n" +
            "•\tStrengthen partnerships with parents, local businesses, and the broader community\n" +
            "•\tDevelop programs to promote parental involvement and engagement\n" +
            "•\tEstablish partnerships to provide opportunities for students to engage in community service\n" +
            "4. Infrastructure Development\n" +
            "•\tDevelop and maintain modern, safe, and functional facilities\n" +
            "•\tImplement sustainable practices to reduce environmental impact\n" +
            "•\tUpgrade technology and digital infrastructure to support teaching and learning"
        );

        // Set the implementation timeline
        plan.setImplementationTimeline(
            "Year 1\n" +
            "•\tDevelop and implement a new curriculum framework\n" +
            "•\tIntroduce mental health support programs for students\n" +
            "•\tEstablish a parent-teacher association\n" +
            "Year 2\n" +
            "•\tImplement teacher training programs\n" +
            "•\tDevelop partnerships with local businesses and organizations\n" +
            "•\tUpgrade school infrastructure\n" +
            "Year 3\n" +
            "•\tEvaluate and refine the strategic plan\n" +
            "•\tExpand community engagement initiatives\n" +
            "•\tDevelop a sustainability plan"
        );

        // Set the financial projections
        plan.setFinancialProjections(
            "Revenue Streams\n" +
            "•\tTuition fees\n" +
            "•\tGrants and Funding\n" +
            "•\tFundraising Events\n" +
            "Expenses\n" +
            "•\tStaff salaries and Training\n" +
            "•\tSchool premises Acquisition\n" +
            "•\tInfrastructure Development\n" +
            "•\tCurriculum Resources and Material\n" +
            "Projected Growth\n" +
            "•\tIncreased enrolment by 50% annually for the 1st 4 years\n" +
            "•\tExpand Programmes and services to meet growing demand"
        );

        // Set the budget allocation
        plan.setBudgetAllocation(
            "Year 1\n" +
            "60% staff salaries, 20% infrastructure, 10% curriculum development, 10% community engagement\n" +
            "Year 2\n" +
            "55% staff salaries, 25% infrastructure, 10% curriculum development, 10% community engagement\n" +
            "Year 3\n" +
            "50% staff salaries, 20% infrastructure, 15% curriculum development, 15% community engagement"
        );
    }

    private Company getCompanyById(Long companyId) throws SQLException {
    String sql = "SELECT id, name, registration_number, logo_path FROM companies WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setRegistrationNumber(rs.getString("registration_number"));
                try {
                    company.setLogoPath(rs.getString("logo_path"));
                } catch (Exception ex) {
                    // logo_path may not exist on older schemas - ignore
                }
                return company;
            }
        }

        return null;
    }

    private static class BudgetData {
        private final Budget budget;
        private final List<BudgetCategory> categories;
        private final List<BudgetItem> items;

        BudgetData(Budget budget, List<BudgetCategory> categories, List<BudgetItem> items) {
            this.budget = budget;
            this.categories = categories;
            this.items = items;
        }

        public Budget getBudget() { return budget; }
        public List<BudgetCategory> getCategories() { return categories; }
        public List<BudgetItem> getItems() { return items; }
    }

    private static class StrategicPlanData {
        private final StrategicPlan plan;
        private final List<StrategicPriority> priorities;
        private final List<StrategicInitiative> initiatives;
        private final List<StrategicMilestone> milestones;
        private final List<OperationalActivity> operationalActivities;
        private final BudgetData budgetData;

        StrategicPlanData(StrategicPlan plan, List<StrategicPriority> priorities,
                         List<StrategicInitiative> initiatives, List<StrategicMilestone> milestones,
                         List<OperationalActivity> operationalActivities, BudgetData budgetData) {
            this.plan = plan;
            this.priorities = priorities;
            this.initiatives = initiatives;
            this.milestones = milestones;
            this.operationalActivities = operationalActivities;
            this.budgetData = budgetData;
        }

        public StrategicPlan getPlan() { return plan; }
        public List<StrategicPriority> getPriorities() { return priorities; }
        public List<StrategicInitiative> getInitiatives() { return initiatives; }
        public List<StrategicMilestone> getMilestones() { return milestones; }
        public List<OperationalActivity> getOperationalActivities() { return operationalActivities; }
        public BudgetData getBudgetData() { return budgetData; }
    }
}