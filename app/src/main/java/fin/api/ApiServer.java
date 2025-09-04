package fin.api;

import static spark.Spark.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fin.service.*;
import fin.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API Server for FIN Financial Management System
 * Provides RESTful endpoints for frontend integration
 * 
 * Endpoints:
 * - GET  /api/v1/health                          - Health check
 * - GET  /api/v1/companies                       - List all companies
 * - POST /api/v1/companies                       - Create new company
 * - GET  /api/v1/companies/{id}/fiscal-periods   - Get fiscal periods for company
 * - GET  /api/v1/companies/{id}/transactions     - Get transactions for company
 * - POST /api/v1/companies/{id}/upload          - Upload and process bank statement
 */
public class ApiServer {
    private final Gson gson;
    private final CompanyService companyService;
    private final CsvImportService csvImportService;
    private final ReportService reportService;
    private final BankStatementProcessingService bankStatementService;
    
    // In-memory store for API session data (replace with proper session management)
    private final Map<String, Object> sessionStore = new ConcurrentHashMap<>();
    
    public ApiServer() {
        this.gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setPrettyPrinting()
            .create();
            
        String dbUrl = "jdbc:sqlite:fin_database.db";
        this.companyService = new CompanyService(dbUrl);
        this.csvImportService = new CsvImportService(dbUrl, companyService);
        this.reportService = new ReportService(dbUrl, csvImportService);
        this.bankStatementService = new BankStatementProcessingService(dbUrl);
    }
    
    public void start() {
        // Set port
        port(8080);
        
        // Configure CORS for frontend connection
        setupCors();
        
        // Configure exception handling
        setupExceptionHandling();
        
        // Setup API routes
        setupRoutes();
        
        // Log startup
        System.out.println("🚀 FIN API Server started successfully!");
        System.out.println("📊 Health check: http://localhost:8080/api/v1/health");
        System.out.println("🏢 Companies API: http://localhost:8080/api/v1/companies");
        System.out.println("📈 Full API documentation: http://localhost:8080/api/v1/docs");
        System.out.println("🌐 Ready for frontend connections from: http://localhost:3000");
    }
    
    private void setupCors() {
        // CORS configuration for development
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "http://localhost:3000");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");
            response.header("Access-Control-Allow-Credentials", "true");
        });
        
        // Handle preflight requests
        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });
    }
    
    private void setupExceptionHandling() {
        // Global exception handler
        exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            response.type("application/json");
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", exception.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            error.put("path", request.pathInfo());
            
            response.body(gson.toJson(error));
            
            // Log the error
            System.err.println("❌ API Error: " + exception.getMessage());
            exception.printStackTrace();
        });
        
        // Not found handler
        notFound((request, response) -> {
            response.type("application/json");
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "The requested endpoint does not exist");
            error.put("path", request.pathInfo());
            error.put("timestamp", System.currentTimeMillis());
            
            return gson.toJson(error);
        });
    }
    
    private void setupRoutes() {
        // API version prefix
        path("/api/v1", () -> {
            
            // Health check endpoint
            get("/health", (req, res) -> {
                res.type("application/json");
                
                Map<String, Object> health = new HashMap<>();
                health.put("status", "OK");
                health.put("timestamp", System.currentTimeMillis());
                health.put("version", "1.0.0");
                health.put("service", "FIN Financial Management API");
                
                // Test database connection
                try {
                    var companies = companyService.getAllCompanies();
                    health.put("database", "connected");
                    health.put("companies_count", companies.size());
                } catch (Exception e) {
                    health.put("database", "error");
                    health.put("database_error", e.getMessage());
                }
                
                return gson.toJson(health);
            });
            
            // API documentation endpoint
            get("/docs", (req, res) -> {
                res.type("application/json");
                
                Map<String, Object> docs = new HashMap<>();
                docs.put("service", "FIN Financial Management API");
                docs.put("version", "1.0.0");
                docs.put("description", "REST API for financial data management and bank statement processing");
                
                Map<String, Object> endpoints = new HashMap<>();
                endpoints.put("GET /api/v1/health", "Health check and system status");
                endpoints.put("GET /api/v1/companies", "List all companies");
                endpoints.put("POST /api/v1/companies", "Create new company");
                endpoints.put("GET /api/v1/companies/{id}/fiscal-periods", "Get fiscal periods for company");
                endpoints.put("GET /api/v1/companies/{id}/transactions", "Get transactions for company");
                endpoints.put("POST /api/v1/companies/{id}/upload", "Upload bank statement for processing");
                
                docs.put("endpoints", endpoints);
                docs.put("frontend_url", "http://localhost:3000");
                docs.put("cors_enabled", true);
                
                return gson.toJson(docs);
            });
            
            // Companies endpoints
            get("/companies", (req, res) -> {
                res.type("application/json");
                
                try {
                    var companies = companyService.getAllCompanies();
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", companies);
                    response.put("count", companies.size());
                    response.put("timestamp", System.currentTimeMillis());
                    
                    return gson.toJson(response);
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Failed to fetch companies",
                        "message", e.getMessage()
                    ));
                }
            });
            
            post("/companies", (req, res) -> {
                res.type("application/json");
                
                try {
                    // Parse company data from request body
                    Map<String, Object> companyData = gson.fromJson(req.body(), Map.class);
                    
                    String name = (String) companyData.get("name");
                    String registrationNumber = (String) companyData.get("registrationNumber");
                    String taxNumber = (String) companyData.get("taxNumber");
                    String address = (String) companyData.get("address");
                    String contactEmail = (String) companyData.get("contactEmail");
                    String contactPhone = (String) companyData.get("contactPhone");
                    
                    if (name == null || name.trim().isEmpty()) {
                        res.status(400);
                        return gson.toJson(Map.of(
                            "success", false,
                            "error", "Company name is required"
                        ));
                    }
                    
                    // Create company using service
                    Company company = new Company();
                    company.setName(name);
                    if (registrationNumber != null) company.setRegistrationNumber(registrationNumber);
                    if (taxNumber != null) company.setTaxNumber(taxNumber);
                    if (address != null) company.setAddress(address);
                    if (contactEmail != null) company.setContactEmail(contactEmail);
                    if (contactPhone != null) company.setContactPhone(contactPhone);
                    
                    Company createdCompany = companyService.createCompany(company);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", createdCompany);
                    response.put("message", "Company created successfully");
                    response.put("timestamp", System.currentTimeMillis());
                    
                    res.status(201);
                    return gson.toJson(response);
                    
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Failed to create company",
                        "message", e.getMessage()
                    ));
                }
            });
            
            // Fiscal periods endpoint
            get("/companies/:id/fiscal-periods", (req, res) -> {
                res.type("application/json");
                
                try {
                    Long companyId = Long.parseLong(req.params(":id"));
                    var fiscalPeriods = companyService.getFiscalPeriodsByCompany(companyId);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", fiscalPeriods);
                    response.put("company_id", companyId);
                    response.put("count", fiscalPeriods.size());
                    response.put("timestamp", System.currentTimeMillis());
                    
                    return gson.toJson(response);
                    
                } catch (NumberFormatException e) {
                    res.status(400);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Invalid company ID format"
                    ));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Failed to fetch fiscal periods",
                        "message", e.getMessage()
                    ));
                }
            });
            
            // Transactions endpoint (placeholder)
            get("/companies/:id/transactions", (req, res) -> {
                res.type("application/json");
                
                try {
                    Long companyId = Long.parseLong(req.params(":id"));
                    
                    // For now, return empty array - will be populated with actual transaction data
                    List<Object> transactions = new ArrayList<>();
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", transactions);
                    response.put("company_id", companyId);
                    response.put("count", transactions.size());
                    response.put("timestamp", System.currentTimeMillis());
                    response.put("note", "Transaction data integration coming soon");
                    
                    return gson.toJson(response);
                    
                } catch (NumberFormatException e) {
                    res.status(400);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Invalid company ID format"
                    ));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Failed to fetch transactions",
                        "message", e.getMessage()
                    ));
                }
            });
            
            // File upload endpoint (placeholder)
            post("/companies/:id/upload", (req, res) -> {
                res.type("application/json");
                
                try {
                    Long companyId = Long.parseLong(req.params(":id"));
                    
                    // For now, return success response - file upload implementation coming soon
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "File upload endpoint ready");
                    response.put("company_id", companyId);
                    response.put("note", "File upload implementation coming soon");
                    response.put("timestamp", System.currentTimeMillis());
                    
                    return gson.toJson(response);
                    
                } catch (NumberFormatException e) {
                    res.status(400);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Invalid company ID format"
                    ));
                } catch (Exception e) {
                    res.status(500);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Failed to process upload",
                        "message", e.getMessage()
                    ));
                }
            });
        });
    }
    
    public void stop() {
        spark.Spark.stop();
        System.out.println("🛑 FIN API Server stopped");
    }
}
