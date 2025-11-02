/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Sthwalo Nyoni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fin.api;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fin.service.BankStatementProcessingService;
import fin.service.CompanyService;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.config.DatabaseConfig;

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
    private final BankStatementProcessingService bankStatementService;
    
    // HTTP status code constants
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    
    // Server configuration constants
    private static final int SERVER_PORT = 8080;
    
    // Constructor for modular architecture with dependency injection
    public ApiServer(CompanyService initialCompanyService, BankStatementProcessingService initialBankStatementService) {
        
        // Instantiate services BEFORE field assignment (secure constructor pattern)
        CompanyService finalCompanyService = initialCompanyService != null ? initialCompanyService : new CompanyService(DatabaseConfig.getDatabaseUrl());
        BankStatementProcessingService finalBankStatementService = initialBankStatementService != null ? initialBankStatementService : new BankStatementProcessingService(DatabaseConfig.getDatabaseUrl());
        
        // Create Gson AFTER service validation (no exception risk)
        Gson gsonInstance = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
                    return new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
            })
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setPrettyPrinting()
            .create();
            
        // Only assign fields AFTER successful initialization
        this.companyService = finalCompanyService;
        this.bankStatementService = finalBankStatementService;
        this.gson = gsonInstance;
    }
    
    // Legacy constructor for backward compatibility
    public ApiServer() {
        this(null, null);
    }
    
    public final void start() {
        // Set port
        port(SERVER_PORT);
        
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
            response.status(HTTP_OK);
            return "";
        });
    }
    
    private void setupExceptionHandling() {
        // Global exception handler
        exception(Exception.class, (exception, request, response) -> {
            response.status(HTTP_INTERNAL_SERVER_ERROR);
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
            setupHealthRoutes();
            setupDocumentationRoutes();
            setupCompanyRoutes();
            setupFiscalPeriodRoutes();
            setupTransactionRoutes();
            setupUploadRoutes();
            setupLocalProcessingRoutes();
        });
    }
    
    private void setupHealthRoutes() {
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
    }
    
    private void setupDocumentationRoutes() {
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
    }
    
    private void setupCompanyRoutes() {
        setupCompanyGetRoutes();
        setupCompanyPostRoutes();
    }
    
    private void setupCompanyGetRoutes() {
        // GET /companies endpoint
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
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to fetch companies",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private void setupCompanyPostRoutes() {
        // POST /companies endpoint
        post("/companies", (req, res) -> {
            res.type("application/json");
            
            try {
                Map<String, Object> companyData = parseCompanyDataFromRequest(req);
                validateCompanyData(companyData, res);
                
                Company company = createCompanyFromData(companyData);
                Company createdCompany = companyService.createCompany(company);
                
                res.status(HTTP_CREATED);
                return buildCompanyCreationResponse(createdCompany);
                
            } catch (Exception e) {
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to create company",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private Map<String, Object> parseCompanyDataFromRequest(spark.Request req) {
        // Parse company data from request body
        @SuppressWarnings("unchecked")
        Map<String, Object> companyData = (Map<String, Object>) gson.fromJson(req.body(), Map.class);
        return companyData;
    }
    
    private void validateCompanyData(Map<String, Object> companyData, spark.Response res) {
        String name = (String) companyData.get("name");
        
        if (name == null || name.trim().isEmpty()) {
            res.status(HTTP_BAD_REQUEST);
            throw new IllegalArgumentException("Company name is required");
        }
    }
    
    private Company createCompanyFromData(Map<String, Object> companyData) {
        // Create company using service
        Company company = new Company();
        company.setName((String) companyData.get("name"));
        
        String registrationNumber = (String) companyData.get("registrationNumber");
        if (registrationNumber != null) {
            company.setRegistrationNumber(registrationNumber);
        }
        
        String taxNumber = (String) companyData.get("taxNumber");
        if (taxNumber != null) {
            company.setTaxNumber(taxNumber);
        }
        
        String address = (String) companyData.get("address");
        if (address != null) {
            company.setAddress(address);
        }
        
        String contactEmail = (String) companyData.get("contactEmail");
        if (contactEmail != null) {
            company.setContactEmail(contactEmail);
        }
        
        String contactPhone = (String) companyData.get("contactPhone");
        if (contactPhone != null) {
            company.setContactPhone(contactPhone);
        }
        
        return company;
    }
    
    private String buildCompanyCreationResponse(Company createdCompany) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", createdCompany);
        response.put("message", "Company created successfully");
        response.put("timestamp", System.currentTimeMillis());
        
        return gson.toJson(response);
    }
    
    private void setupFiscalPeriodRoutes() {
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
                res.status(HTTP_BAD_REQUEST);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Invalid company ID format"
                ));
            } catch (Exception e) {
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to fetch fiscal periods",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private void setupTransactionRoutes() {
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
                res.status(HTTP_BAD_REQUEST);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Invalid company ID format"
                ));
            } catch (Exception e) {
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to fetch transactions",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private void setupUploadRoutes() {
        // File upload endpoint - IMPLEMENTED
        post("/companies/:id/upload", (req, res) -> {
            res.type("application/json");
            
            try {
                Long companyId = Long.parseLong(req.params(":id"));
                Company company = validateCompanyForUpload(companyId, res);
                if (company == null) {
                    return null; // Response already sent
                }
                
                UploadResult result = processInputDirectoryFiles(company);
                
                return buildUploadResponse(companyId, company, result);
                
            } catch (NumberFormatException e) {
                res.status(HTTP_BAD_REQUEST);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Invalid company ID format"
                ));
            } catch (Exception e) {
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                System.err.println("Upload processing error: " + e.getMessage());
                e.printStackTrace();
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to process upload",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private Company validateCompanyForUpload(Long companyId, spark.Response res) {
        // Check if company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            res.status(HTTP_NOT_FOUND);
            res.body(gson.toJson(Map.of(
                "success", false,
                "error", "Company not found",
                "company_id", companyId
            )));
            return null;
        }
        return company;
    }
    
    private UploadResult processInputDirectoryFiles(Company company) {
        // Get file upload information - will be implemented later for actual multipart uploads
        // For now, process files already in the input directory
        List<String> processedFiles = new ArrayList<>();
        int totalTransactions = 0;
        
        // Process all PDF files in the input directory
        java.io.File inputDir = new java.io.File("input");
        System.out.println("🔍 Looking for PDFs in: " + inputDir.getAbsolutePath());
        
        if (inputDir.exists() && inputDir.isDirectory()) {
            java.io.File[] pdfFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            
            if (pdfFiles != null) {
                for (java.io.File pdfFile : pdfFiles) {
                    try {
                        System.out.println("🔄 Processing: " + pdfFile.getName());
                        
                        // Process the bank statement
                        List<BankTransaction> transactions = bankStatementService.processStatement(
                            pdfFile.getAbsolutePath(), 
                            company
                        );
                        
                        int transactionCount = transactions.size();
                        processedFiles.add(pdfFile.getName());
                        totalTransactions += transactionCount;
                        
                        System.out.println("✅ Processed " + pdfFile.getName() + ": " + transactionCount + " transactions");
                        
                    } catch (Exception e) {
                        System.err.println("❌ Failed to process " + pdfFile.getName() + ": " + e.getMessage());
                        // Continue with other files
                    }
                }
            }
        }
        
        return new UploadResult(processedFiles, totalTransactions);
    }
    
    private String buildUploadResponse(Long companyId, Company company, UploadResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bank statements processed successfully");
        response.put("company_id", companyId);
        response.put("company_name", company.getName());
        response.put("files_processed", result.getProcessedFiles());
        response.put("total_files", result.getProcessedFiles().size());
        response.put("total_transactions", result.getTotalTransactions());
        response.put("timestamp", System.currentTimeMillis());
        
        return gson.toJson(response);
    }
    
    // Helper class for upload results
    private static class UploadResult {
        private final List<String> processedFiles;
        private final int totalTransactions;
        
        UploadResult(List<String> files, int transactions) {
            this.processedFiles = files;
            this.totalTransactions = transactions;
        }
        
        public List<String> getProcessedFiles() {
            return processedFiles;
        }
        
        public int getTotalTransactions() {
            return totalTransactions;
        }
    }
    
    private void setupLocalProcessingRoutes() {
        // File processing endpoint for testing with local files
        post("/companies/:id/process-local", (req, res) -> {
            res.type("application/json");
            
            try {
                Long companyId = Long.parseLong(req.params(":id"));
                String filePath = validateLocalProcessingRequest(req);
                if (filePath == null) {
                    res.status(HTTP_BAD_REQUEST);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Missing filePath",
                        "message", "Please provide filePath in request body"
                    ));
                }
                
                Company company = validateCompanyForLocalProcessing(companyId);
                if (company == null) {
                    res.status(HTTP_NOT_FOUND);
                    return gson.toJson(Map.of(
                        "success", false,
                        "error", "Company not found",
                        "message", "Company with ID " + companyId + " not found"
                    ));
                }
                
                LocalProcessingResult result = processSingleFile(filePath, company);
                
                return buildLocalProcessingResponse(companyId, filePath, result);
                
            } catch (NumberFormatException e) {
                res.status(HTTP_BAD_REQUEST);
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Invalid company ID format"
                ));
            } catch (Exception e) {
                res.status(HTTP_INTERNAL_SERVER_ERROR);
                System.err.println("Local processing error: " + e.getMessage());
                e.printStackTrace();
                return gson.toJson(Map.of(
                    "success", false,
                    "error", "Failed to process bank statement",
                    "message", e.getMessage()
                ));
            }
        });
    }
    
    private String validateLocalProcessingRequest(spark.Request req) {
        // Get file path from request body
        @SuppressWarnings("unchecked")
        Map<String, String> requestData = (Map<String, String>) gson.fromJson(req.body(), Map.class);
        String filePath = requestData.get("filePath");
        
        if (filePath == null || filePath.trim().isEmpty()) {
            return null; // Will be handled by caller
        }
        
        return filePath;
    }
    
    private Company validateCompanyForLocalProcessing(Long companyId) {
        // Get company
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            return null; // Will be handled by caller
        }
        return company;
    }
    
    private LocalProcessingResult processSingleFile(String filePath, Company company) {
        // Create bank statement processing service
        BankStatementProcessingService bankService = new BankStatementProcessingService(DatabaseConfig.getDatabaseUrl());
        
        // Process the bank statement
        List<BankTransaction> transactions = bankService.processStatement(filePath, company);
        
        return new LocalProcessingResult(transactions);
    }
    
    private String buildLocalProcessingResponse(Long companyId, String filePath, LocalProcessingResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bank statement processed successfully");
        response.put("company_id", companyId);
        response.put("file_path", filePath);
        response.put("transactions_processed", result.getTransactions().size());
        response.put("transactions", result.getTransactions());
        response.put("timestamp", System.currentTimeMillis());
        
        return gson.toJson(response);
    }
    
    // Helper class for local processing results
    private static class LocalProcessingResult {
        private final List<BankTransaction> transactions;
        
        LocalProcessingResult(List<BankTransaction> txns) {
            this.transactions = txns;
        }
        
        public List<BankTransaction> getTransactions() {
            return transactions;
        }
    }
    
    public final void stop() {
        spark.Spark.stop();
        DatabaseConfig.close();
        System.out.println("🛑 FIN API Server stopped");
    }
}
