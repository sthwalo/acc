package fin.service;

import fin.model.Budget;
import fin.model.BudgetCategory;
import fin.model.BudgetItem;
import fin.model.BudgetMonthlyAllocation;
import fin.model.Company;
import fin.model.StrategicPlan;
import fin.model.StrategicPriority;
import fin.model.StrategicInitiative;
import fin.model.StrategicMilestone;
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
 * Budget Report Service for FIN Financial Management System
 * Handles generation of budget reports in PDF format for financial planning and reporting
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
    private static final float LINE_SPACING_LARGE = PdfFormattingUtils.LINE_SPACING_LARGE;
    private static final float LINE_SPACING_NORMAL = PdfFormattingUtils.LINE_SPACING_NORMAL;

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
     * Generate a comprehensive budget summary report in PDF format
     */
    public void generateBudgetSummaryReport(Long companyId) throws IOException, SQLException {
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

        // Generate PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createBudgetReportHeader(contentStream, company, budget);
                createBudgetSummary(contentStream, budgetData);
                createBudgetCategories(contentStream, budgetData);
                createBudgetItems(contentStream, budgetData);
                createFooter(contentStream);
            }

            document.save(reportPath.toFile());
        }

        System.out.println("✅ Budget summary report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Generate strategic planning report in PDF format
     */
    public void generateStrategicPlanReport(Long companyId) throws IOException, SQLException {
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

        // Generate PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createStrategicReportHeader(contentStream, company, strategicData);
                createStrategicVision(contentStream, strategicData);
                createStrategicPriorities(contentStream, strategicData);
                createStrategicInitiatives(contentStream, strategicData);
                createStrategicMilestones(contentStream, strategicData);
                createFooter(contentStream);
            }

            document.save(reportPath.toFile());
        }

        System.out.println("✅ Strategic plan report generated: " + reportPath.toAbsolutePath());
    }

    /**
     * Generate budget vs actual comparison report in PDF format
     */
    public void generateBudgetVsActualReport(Long companyId) throws IOException, SQLException {
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

        // Generate PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                createBudgetVsActualHeader(contentStream, company, budget);
                createBudgetVsActualSummary(contentStream, budgetData);
                createBudgetVsActualDetails(contentStream, budgetData);
                createFooter(contentStream);
            }

            document.save(reportPath.toFile());
        }

        System.out.println("✅ Budget vs actual report generated: " + reportPath.toAbsolutePath());
    }

    private void createBudgetReportHeader(PDPageContentStream contentStream, Company company, Budget budget) throws IOException {
        String subtitle = "Budget Year: " + budget.getBudgetYear() + " | Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PdfFormattingUtils.drawHeaderSection(contentStream, "BUDGET SUMMARY REPORT", company.getName(), subtitle, TITLE_Y);
    }

    private void createBudgetSummary(PDPageContentStream contentStream, BudgetData budgetData) throws IOException {
        // Create summary metrics for the summary box
        String[][] summaryMetrics = {
            {"Total Revenue", "R" + budgetData.getBudget().getTotalRevenue().setScale(2, RoundingMode.HALF_UP)},
            {"Total Expenses", "R" + budgetData.getBudget().getTotalExpenses().setScale(2, RoundingMode.HALF_UP)},
            {"Net Budget", "R" + budgetData.getBudget().getTotalRevenue().subtract(budgetData.getBudget().getTotalExpenses()).setScale(2, RoundingMode.HALF_UP)},
            {"Budget Categories", String.valueOf(budgetData.getCategories().size())},
            {"Budget Items", String.valueOf(budgetData.getItems().size())}
        };

        PdfFormattingUtils.drawSummaryBox(contentStream, summaryMetrics, SUMMARY_Y, 120f);
    }

    private void createBudgetCategories(PDPageContentStream contentStream, BudgetData budgetData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "BUDGET CATEGORIES", CATEGORY_Y, 180f);

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

        PdfFormattingUtils.drawTable(contentStream, headers, tableData, currentY, 20f);
    }

    private void createBudgetItems(PDPageContentStream contentStream, BudgetData budgetData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "BUDGET ITEMS", ITEMS_Y, 150f);

        // Prepare table data
        String[] headers = {"Description", "Annual Amount", "Notes"};
        String[][] tableData = new String[Math.min(budgetData.getItems().size(), 10)][]; // Limit to 10 items to prevent overflow

        for (int i = 0; i < Math.min(budgetData.getItems().size(), 10); i++) {
            BudgetItem item = budgetData.getItems().get(i);
            tableData[i] = new String[]{
                item.getDescription(),
                "R" + item.getAnnualAmount().setScale(2, RoundingMode.HALF_UP),
                item.getNotes() != null ? item.getNotes() : ""
            };
        }

        PdfFormattingUtils.drawTable(contentStream, headers, tableData, currentY, 18f);
    }

    private void createStrategicReportHeader(PDPageContentStream contentStream, Company company, StrategicPlanData strategicData) throws IOException {
        String subtitle = "Plan: " + strategicData.getPlan().getTitle() + " | Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PdfFormattingUtils.drawHeaderSection(contentStream, "STRATEGIC PLAN REPORT", company.getName(), subtitle, TITLE_Y);
    }

    private void createStrategicVision(PDPageContentStream contentStream, StrategicPlanData strategicData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "VISION & MISSION", SUMMARY_Y, 80f);

        String[][] visionMission = {
            {"Vision", strategicData.getPlan().getVisionStatement()},
            {"Mission", strategicData.getPlan().getMissionStatement()}
        };

        PdfFormattingUtils.drawKeyValueList(contentStream, visionMission, currentY, 80f);
    }

    private void createStrategicPriorities(PDPageContentStream contentStream, StrategicPlanData strategicData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "STRATEGIC PRIORITIES", CATEGORY_Y, 120f);

        // Prepare table data
        String[] headers = {"Priority", "Name", "Description"};
        String[][] tableData = new String[strategicData.getPriorities().size()][];

        for (int i = 0; i < strategicData.getPriorities().size(); i++) {
            StrategicPriority priority = strategicData.getPriorities().get(i);
            tableData[i] = new String[]{
                String.valueOf(priority.getPriorityOrder()),
                priority.getName(),
                priority.getDescription()
            };
        }

        PdfFormattingUtils.drawTable(contentStream, headers, tableData, currentY, 20f);
    }

    private void createStrategicInitiatives(PDPageContentStream contentStream, StrategicPlanData strategicData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "STRATEGIC INITIATIVES", ITEMS_Y, 150f);

        // Prepare table data - limit to prevent overflow
        String[] headers = {"Initiative", "Budget", "Status", "Period"};
        String[][] tableData = new String[Math.min(strategicData.getInitiatives().size(), 8)][];

        for (int i = 0; i < Math.min(strategicData.getInitiatives().size(), 8); i++) {
            StrategicInitiative initiative = strategicData.getInitiatives().get(i);
            tableData[i] = new String[]{
                initiative.getTitle(),
                "R" + initiative.getBudgetAllocated().setScale(2, RoundingMode.HALF_UP),
                initiative.getStatus(),
                initiative.getStartDate() + " to " + initiative.getEndDate()
            };
        }

        PdfFormattingUtils.drawTable(contentStream, headers, tableData, currentY, 18f);
    }

    private void createStrategicMilestones(PDPageContentStream contentStream, StrategicPlanData strategicData) throws IOException {
        float currentY = PdfFormattingUtils.drawSectionHeader(contentStream, "STRATEGIC MILESTONES", STRATEGIC_Y, 120f);

        // Prepare table data - limit to prevent overflow
        String[] headers = {"Milestone", "Target Date", "Status"};
        String[][] tableData = new String[Math.min(strategicData.getMilestones().size(), 6)][];

        for (int i = 0; i < Math.min(strategicData.getMilestones().size(), 6); i++) {
            StrategicMilestone milestone = strategicData.getMilestones().get(i);
            tableData[i] = new String[]{
                milestone.getTitle(),
                milestone.getTargetDate(),
                milestone.getStatus()
            };
        }

        PdfFormattingUtils.drawTable(contentStream, headers, tableData, currentY, 18f);
    }

    private void createBudgetVsActualHeader(PDPageContentStream contentStream, Company company, Budget budget) throws IOException {
        String subtitle = "Budget Year: " + budget.getBudgetYear() + " | Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PdfFormattingUtils.drawHeaderSection(contentStream, "BUDGET VS ACTUAL REPORT", company.getName(), subtitle, TITLE_Y);
    }

    private void createBudgetVsActualSummary(PDPageContentStream contentStream, BudgetData budgetData) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, SUMMARY_Y);
        contentStream.showText("BUDGET SUMMARY");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, TABLE_Y);
        contentStream.showText("Budgeted Revenue: R" + budgetData.getBudget().getTotalRevenue().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Budgeted Expenses: R" + budgetData.getBudget().getTotalExpenses().setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        BigDecimal netBudget = budgetData.getBudget().getTotalRevenue().subtract(budgetData.getBudget().getTotalExpenses());
        contentStream.showText("Budgeted Net: R" + netBudget.setScale(2, RoundingMode.HALF_UP));
        contentStream.newLineAtOffset(0, -LINE_SPACING_LARGE);
        contentStream.showText("Actual Amount: [Not implemented - would compare with actual transactions]");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Variance: [Not implemented]");
        contentStream.endText();
    }

    private void createBudgetVsActualDetails(PDPageContentStream contentStream, BudgetData budgetData) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, CATEGORY_Y);
        contentStream.showText("BUDGET VS ACTUAL COMPARISON");
        contentStream.endText();

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        contentStream.beginText();
        contentStream.newLineAtOffset(PAGE_MARGIN_LEFT, CATEGORY_Y - LINE_SPACING_NORMAL);
        contentStream.showText("Budget vs Actual comparison feature coming soon!");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("This will compare budgeted amounts with actual transaction data.");
        contentStream.newLineAtOffset(0, -LINE_SPACING_NORMAL);
        contentStream.showText("Currently showing budgeted amounts with placeholders for actual spending.");
        contentStream.endText();
    }

    private void createFooter(PDPageContentStream contentStream) throws IOException {
        PdfFormattingUtils.drawFooter(contentStream,
            "Generated using FIN Financial Management System",
            "Confidential - For internal use only");
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

        return new StrategicPlanData(plan, priorities, initiatives, milestones);
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

        StrategicPlanData(StrategicPlan plan, List<StrategicPriority> priorities,
                         List<StrategicInitiative> initiatives, List<StrategicMilestone> milestones) {
            this.plan = plan;
            this.priorities = priorities;
            this.initiatives = initiatives;
            this.milestones = milestones;
        }

        public StrategicPlan getPlan() { return plan; }
        public List<StrategicPriority> getPriorities() { return priorities; }
        public List<StrategicInitiative> getInitiatives() { return initiatives; }
        public List<StrategicMilestone> getMilestones() { return milestones; }
    }
}