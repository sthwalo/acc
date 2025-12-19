# 🚀 Production Deployment & Full-Stack Integration Strategy

## Overview

You now have a **complete backend financial system** ready for production deployment. This document outlines the strategy for deploying the current system and integrating it with your frontend (drimacc) for a complete full-stack solution.

## 🏗️ System Architecture

### Current State:
- **Backend/Core System** (acc): Complete Java-based financial management system
- **Frontend System** (drimacc): Frontend-focused implementation
- **Target**: Integrated full-stack production system

### Deployment Architecture:
```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Frontend (Web)    │    │   API Gateway       │    │   Backend Services  │
│   drimacc repo      │◄──►│   (Optional)        │◄──►│   acc repo          │
│   - React/Vue/etc   │    │   - Authentication  │    │   - Java Services   │
│   - User Interface  │    │   - Rate Limiting   │    │   - PostgreSQL DB   │
│   - Dashboard       │    │   - CORS handling   │    │   - PDF Processing  │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
```

## 🎯 Production Deployment Plan

### Phase 1: Backend Production Deployment (2-3 hours)

#### 1.1 PostgreSQL Migration (Following our migration guide)
```bash
# Step 1: Set up PostgreSQL
brew install postgresql@15
brew services start postgresql@15

# Step 2: Create production database
createdb fin_production
psql fin_production -c "CREATE USER fin_prod WITH PASSWORD 'REPLACE_WITH_SECURE_PROD_PASSWORD';"
psql fin_production -c "GRANT ALL PRIVILEGES ON DATABASE fin_production TO fin_prod;"

# Step 3: Run schema migration
psql -U fin_prod -d fin_production -f app/src/main/resources/db/migration/V2__PostgreSQL_Schema.sql
```

#### 1.2 Update Production Configuration
```java
// Create app/src/main/java/fin/config/ProductionConfig.java
package fin.config;

public class ProductionConfig {
    public static final String DATABASE_URL = System.getenv("DATABASE_URL");
    public static final String DATABASE_USER = System.getenv("DATABASE_USER");
    public static final String DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD");
    
    // API Configuration
    public static final int API_PORT = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "8080"));
    public static final String CORS_ORIGIN = System.getenv()
        .getOrDefault("CORS_ORIGIN", "http://localhost:3000");
}
```

#### 1.3 Add REST API Layer
```gradle
// Update app/build.gradle.kts
dependencies {
    // Existing dependencies...
    
    // Add REST API capabilities
    implementation("com.sparkjava:spark-core:2.9.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    
    // CORS support
    implementation("org.eclipse.jetty:jetty-server:11.0.15")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.15")
}
```

### Phase 2: REST API Development (4-6 hours)

#### 2.1 Create API Server
```java
// Create app/src/main/java/fin/api/ApiServer.java
package fin.api;

import static spark.Spark.*;
import com.google.gson.Gson;
import fin.service.*;
import fin.config.ProductionConfig;

public class ApiServer {
    private final Gson gson = new Gson();
    private final CompanyService companyService;
    private final CsvImportService csvImportService;
    private final ReportService reportService;
    // ... other services
    
    public ApiServer() {
        this.companyService = new CompanyService(ProductionConfig.DATABASE_URL);
        this.csvImportService = new CsvImportService(ProductionConfig.DATABASE_URL, companyService);
        // ... initialize other services
    }
    
    public void start() {
        port(ProductionConfig.API_PORT);
        
        // CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", ProductionConfig.CORS_ORIGIN);
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });
        
        // API Routes
        path("/api/v1", () -> {
            // Companies
            get("/companies", (req, res) -> {
                res.type("application/json");
                return gson.toJson(companyService.getAllCompanies());
            });
            
            post("/companies", (req, res) -> {
                res.type("application/json");
                var company = gson.fromJson(req.body(), Company.class);
                return gson.toJson(companyService.createCompany(company));
            });
            
            // Fiscal Periods
            get("/companies/:id/fiscal-periods", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                return gson.toJson(companyService.getFiscalPeriodsByCompany(companyId));
            });
            
            // Accounts
            get("/companies/:id/accounts", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                return gson.toJson(csvImportService.getAccountService().getAccountsByCompany(companyId));
            });
            
            // Bank Transactions
            get("/companies/:id/transactions", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                return gson.toJson(csvImportService.getTransactionsByCompany(companyId));
            });
            
            // CSV Import
            post("/companies/:id/import-csv", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                // Handle file upload and CSV import
                return gson.toJson(Map.of("status", "success", "message", "CSV imported"));
            });
            
            // Reports
            get("/companies/:id/reports/trial-balance", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                Long fiscalPeriodId = Long.parseLong(req.queryParams("fiscal_period_id"));
                return gson.toJson(reportService.generateTrialBalance(companyId, fiscalPeriodId));
            });
            
            get("/companies/:id/reports/income-statement", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                Long fiscalPeriodId = Long.parseLong(req.queryParams("fiscal_period_id"));
                return gson.toJson(reportService.generateIncomeStatement(companyId, fiscalPeriodId));
            });
            
            get("/companies/:id/reports/balance-sheet", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                Long fiscalPeriodId = Long.parseLong(req.queryParams("fiscal_period_id"));
                return gson.toJson(reportService.generateBalanceSheet(companyId, fiscalPeriodId));
            });
        });
        
        // Health check
        get("/health", (req, res) -> "OK");
        
        System.out.println("API Server started on port " + ProductionConfig.API_PORT);
    }
}
```

#### 2.2 Update Main Application
```java
// Update app/src/main/java/fin/App.java
public class App {
    public static void main(String[] args) {
        if (args.length > 0 && "api".equals(args[0])) {
            // Start API server
            new ApiServer().start();
        } else {
            // Start console application (existing functionality)
            new App().showMenu();
        }
    }
    
    // ... existing console application code
}
```

### Phase 3: Frontend Integration Strategy (3-4 hours)

#### 3.1 API Client for Frontend
```javascript
// For your drimacc frontend - create api/financeApi.js
class FinanceApi {
    constructor(baseUrl = 'http://localhost:8080/api/v1') {
        this.baseUrl = baseUrl;
    }
    
    async request(endpoint, options = {}) {
        const response = await fetch(`${this.baseUrl}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            ...options,
        });
        
        if (!response.ok) {
            throw new Error(`API Error: ${response.statusText}`);
        }
        
        return response.json();
    }
    
    // Companies
    async getCompanies() {
        return this.request('/companies');
    }
    
    async createCompany(company) {
        return this.request('/companies', {
            method: 'POST',
            body: JSON.stringify(company),
        });
    }
    
    // Fiscal Periods
    async getFiscalPeriods(companyId) {
        return this.request(`/companies/${companyId}/fiscal-periods`);
    }
    
    // Accounts
    async getAccounts(companyId) {
        return this.request(`/companies/${companyId}/accounts`);
    }
    
    // Transactions
    async getTransactions(companyId) {
        return this.request(`/companies/${companyId}/transactions`);
    }
    
    async importCsv(companyId, file) {
        const formData = new FormData();
        formData.append('file', file);
        
        return fetch(`${this.baseUrl}/companies/${companyId}/import-csv`, {
            method: 'POST',
            body: formData,
        });
    }
    
    // Reports
    async getTrialBalance(companyId, fiscalPeriodId) {
        return this.request(`/companies/${companyId}/reports/trial-balance?fiscal_period_id=${fiscalPeriodId}`);
    }
    
    async getIncomeStatement(companyId, fiscalPeriodId) {
        return this.request(`/companies/${companyId}/reports/income-statement?fiscal_period_id=${fiscalPeriodId}`);
    }
    
    async getBalanceSheet(companyId, fiscalPeriodId) {
        return this.request(`/companies/${companyId}/reports/balance-sheet?fiscal_period_id=${fiscalPeriodId}`);
    }
}

export default FinanceApi;
```

#### 3.2 Frontend Integration Examples
```javascript
// Example React component for your drimacc frontend
import React, { useState, useEffect } from 'react';
import FinanceApi from '../api/financeApi';

const Dashboard = () => {
    const [companies, setCompanies] = useState([]);
    const [selectedCompany, setSelectedCompany] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const api = new FinanceApi();
    
    useEffect(() => {
        loadCompanies();
    }, []);
    
    const loadCompanies = async () => {
        try {
            const companiesData = await api.getCompanies();
            setCompanies(companiesData);
            if (companiesData.length > 0) {
                setSelectedCompany(companiesData[0]);
                loadTransactions(companiesData[0].id);
            }
        } catch (error) {
            console.error('Error loading companies:', error);
        }
    };
    
    const loadTransactions = async (companyId) => {
        try {
            const transactionsData = await api.getTransactions(companyId);
            setTransactions(transactionsData);
        } catch (error) {
            console.error('Error loading transactions:', error);
        }
    };
    
    return (
        <div className="dashboard">
            <h1>Financial Dashboard</h1>
            
            {/* Company Selector */}
            <select 
                value={selectedCompany?.id || ''} 
                onChange={(e) => {
                    const company = companies.find(c => c.id === parseInt(e.target.value));
                    setSelectedCompany(company);
                    if (company) loadTransactions(company.id);
                }}
            >
                {companies.map(company => (
                    <option key={company.id} value={company.id}>
                        {company.name}
                    </option>
                ))}
            </select>
            
            {/* Transactions Table */}
            <div className="transactions">
                <h2>Recent Transactions</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Description</th>
                            <th>Debit</th>
                            <th>Credit</th>
                            <th>Balance</th>
                        </tr>
                    </thead>
                    <tbody>
                        {transactions.map(transaction => (
                            <tr key={transaction.id}>
                                <td>{transaction.transactionDate}</td>
                                <td>{transaction.details}</td>
                                <td>{transaction.debitAmount || '-'}</td>
                                <td>{transaction.creditAmount || '-'}</td>
                                <td>{transaction.balance}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Dashboard;
```

## 🚀 Production Deployment Options

### Option A: Single Server Deployment
```bash
# Build the application
./gradlew build

# Run as API server
java -jar app/build/libs/fin-spring.jar api

# Serve frontend (drimacc) via nginx or similar
```

### Option B: Containerized Deployment
```dockerfile
# Dockerfile for backend
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY app/build/libs/fin-spring.jar app.jar
COPY app/src/main/resources/ resources/

EXPOSE 8080
CMD ["java", "-jar", "app.jar", "api"]
# Note: When building the Docker image we copy `fin-spring.jar` to `/app/app.jar` inside the container, so `app.jar` refers to that bundled JAR.
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: fin_production
      POSTGRES_USER: fin_prod
      POSTGRES_PASSWORD: REPLACE_WITH_SECURE_POSTGRES_PASSWORD
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
  
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/fin_production
      DATABASE_USER: fin_prod
      DATABASE_PASSWORD: secure_password
    depends_on:
      - postgres
  
  frontend:
    # Build from your drimacc repository
    build: ../drimacc
    ports:
      - "3000:3000"
    environment:
      REACT_APP_API_URL: http://localhost:8080/api/v1
    depends_on:
      - backend

volumes:
  postgres_data:
```

### Option C: Cloud Deployment (Heroku/Railway/DigitalOcean)
```bash
# For Heroku deployment
echo "web: java -jar app/build/libs/fin-spring.jar api" > Procfile

# Add PostgreSQL addon
heroku addons:create heroku-postgresql:mini

# Set environment variables
heroku config:set CORS_ORIGIN=https://your-frontend-domain.com
```

## 📊 Scaling Strategy

### Immediate Production (Next 1-2 weeks):
1. ✅ **Deploy current backend** with PostgreSQL
2. ✅ **Add REST API layer** (4-6 hours development)
3. ✅ **Connect frontend** to API endpoints
4. ✅ **Basic authentication** and security

### Short-term Enhancements (1-3 months):
1. **Authentication & Authorization** - JWT tokens, role-based access
2. **File Upload API** - PDF bank statement processing via API
3. **Real-time Updates** - WebSocket for live transaction updates
4. **Caching** - Redis for improved performance
5. **API Documentation** - Swagger/OpenAPI specs

### Long-term Scaling (3-12 months):
1. **Microservices Architecture** - Split into focused services
2. **Message Queues** - Async processing for large imports
3. **Multi-tenancy** - Support multiple organizations
4. **Advanced Analytics** - Business intelligence features
5. **Mobile API** - Support for mobile applications

## 🔒 Security Considerations

### API Security:
```java
// Add authentication middleware
before("/api/*", (request, response) -> {
    String token = request.headers("Authorization");
    if (!isValidToken(token)) {
        halt(401, "Unauthorized");
    }
});
```

### Database Security:
```sql
-- Create read-only user for reporting
CREATE USER fin_readonly WITH PASSWORD 'readonly_password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO fin_readonly;

-- Row-level security for multi-tenant data
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
```

## ✅ Next Steps

### Immediate Actions:
1. **Complete PostgreSQL migration** (3.5 hours)
2. **Implement REST API layer** (4-6 hours)
3. **Test API endpoints** with Postman/curl
4. **Connect frontend repository** to new API

### This Week:
1. Deploy to production environment
2. Set up monitoring and logging
3. Implement basic authentication
4. Test full-stack integration

### Next Month:
1. Add advanced features based on user feedback
2. Implement proper CI/CD pipeline
3. Add comprehensive API testing
4. Plan mobile application development

---

**Ready to deploy?** Your backend system is production-ready and the API integration will give you a powerful full-stack financial platform! 🚀

Which deployment option would you like to pursue first?
