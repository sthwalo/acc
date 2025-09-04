# 🚀 How to Run Both Systems Simultaneously

## Overview

This guide shows you how to run your **Java backend (acc)** and **TypeScript frontend (drimacc)** simultaneously for full-stack development and testing.

## 🎯 Quick Start (5 minutes)

### **Terminal Setup (3 terminals needed):**

#### Terminal 1: Java Backend (Port 8080)
```bash
# Navigate to your backend
cd /Users/sthwalonyoni/FIN

# Option A: Run console application (current)
java -jar app/build/libs/app.jar

# Option B: Run as API server (after adding REST API)
java -jar app/build/libs/app.jar api
```

#### Terminal 2: TypeScript Frontend (Port 3000)
```bash
# Navigate to your frontend
cd /path/to/drimacc

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

#### Terminal 3: Database (PostgreSQL)
```bash
# Start PostgreSQL (if using)
brew services start postgresql@15

# Or SQLite (current setup)
# No additional terminal needed - database is file-based
```

## 🔧 Detailed Setup

### **Step 1: Prepare Backend for API Mode**

First, let's add REST API capability to your existing Java backend:

#### 1.1 Update build.gradle.kts
```kotlin
// Add to app/build.gradle.kts dependencies
dependencies {
    // Existing dependencies...
    implementation("org.xerial:sqlite-jdbc:3.36.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.0")
    
    // Add REST API dependencies
    implementation("com.sparkjava:spark-core:2.9.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}
```

#### 1.2 Create API Server Class
```java
// Create app/src/main/java/fin/api/ApiServer.java
package fin.api;

import static spark.Spark.*;
import com.google.gson.Gson;
import fin.service.*;

public class ApiServer {
    private final Gson gson = new Gson();
    private final CompanyService companyService;
    private final CsvImportService csvImportService;
    private final ReportService reportService;
    
    public ApiServer() {
        String dbUrl = "jdbc:sqlite:fin_database.db";
        this.companyService = new CompanyService(dbUrl);
        this.csvImportService = new CsvImportService(dbUrl, companyService);
        this.reportService = new ReportService(dbUrl, companyService);
    }
    
    public void start() {
        port(8080);
        
        // CORS for frontend connection
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "http://localhost:3000");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });
        
        // Handle preflight requests
        options("/*", (request, response) -> {
            response.status(200);
            return "";
        });
        
        // API Routes
        path("/api/v1", () -> {
            // Health check
            get("/health", (req, res) -> {
                res.type("application/json");
                return gson.toJson(Map.of("status", "OK", "timestamp", System.currentTimeMillis()));
            });
            
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
            
            // Transactions (basic endpoint)
            get("/companies/:id/transactions", (req, res) -> {
                res.type("application/json");
                Long companyId = Long.parseLong(req.params(":id"));
                // For now, return empty array - will be populated with actual data
                return gson.toJson(java.util.Collections.emptyList());
            });
        });
        
        System.out.println("🚀 API Server started on http://localhost:8080");
        System.out.println("📊 Health check: http://localhost:8080/api/v1/health");
        System.out.println("🏢 Companies API: http://localhost:8080/api/v1/companies");
    }
}
```

#### 1.3 Update Main App.java
```java
// Update app/src/main/java/fin/App.java main method
public static void main(String[] args) {
    if (args.length > 0 && "api".equals(args[0])) {
        // Start API server mode
        System.out.println("🚀 Starting FIN API Server...");
        new ApiServer().start();
    } else {
        // Start console application (existing functionality)
        System.out.println("🖥️  Starting FIN Console Application...");
        new App().showMenu();
    }
}
```

### **Step 2: Update Frontend for Backend Connection**

#### 2.1 Create API Configuration
```typescript
// Create drimacc/src/config/api.ts
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api/v1',
  TIMEOUT: 10000,
  RETRY_ATTEMPTS: 3,
};

export const ENDPOINTS = {
  HEALTH: '/health',
  COMPANIES: '/companies',
  FISCAL_PERIODS: (companyId: number) => `/companies/${companyId}/fiscal-periods`,
  TRANSACTIONS: (companyId: number) => `/companies/${companyId}/transactions`,
  ACCOUNTS: (companyId: number) => `/companies/${companyId}/accounts`,
} as const;
```

#### 2.2 Create Simple API Client
```typescript
// Create drimacc/src/services/api/simpleClient.ts
import { API_CONFIG, ENDPOINTS } from '../../config/api';

export class SimpleApiClient {
  private baseUrl = API_CONFIG.BASE_URL;

  async get<T>(endpoint: string): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  async post<T>(endpoint: string, data: any): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  // Test connection to backend
  async testConnection(): Promise<boolean> {
    try {
      await this.get(ENDPOINTS.HEALTH);
      return true;
    } catch (error) {
      console.error('Backend connection failed:', error);
      return false;
    }
  }
}

export const apiClient = new SimpleApiClient();
```

#### 2.3 Create Connection Status Component
```typescript
// Create drimacc/src/components/ConnectionStatus/ConnectionStatus.tsx
import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api/simpleClient';

export function ConnectionStatus() {
  const [isConnected, setIsConnected] = useState<boolean | null>(null);
  const [isChecking, setIsChecking] = useState(false);

  const checkConnection = async () => {
    setIsChecking(true);
    try {
      const connected = await apiClient.testConnection();
      setIsConnected(connected);
    } catch (error) {
      setIsConnected(false);
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    checkConnection();
    // Check connection every 30 seconds
    const interval = setInterval(checkConnection, 30000);
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = () => {
    if (isChecking) return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    if (isConnected === null) return 'bg-gray-100 text-gray-800 border-gray-200';
    if (isConnected) return 'bg-green-100 text-green-800 border-green-200';
    return 'bg-red-100 text-red-800 border-red-200';
  };

  const getStatusText = () => {
    if (isChecking) return 'Checking...';
    if (isConnected === null) return 'Unknown';
    if (isConnected) return 'Connected';
    return 'Disconnected';
  };

  const getStatusIcon = () => {
    if (isChecking) return '🔄';
    if (isConnected === null) return '❓';
    if (isConnected) return '✅';
    return '❌';
  };

  return (
    <div className="mb-4">
      <div className={`px-3 py-2 rounded-md border text-sm font-medium ${getStatusColor()}`}>
        <div className="flex items-center justify-between">
          <span>
            {getStatusIcon()} Backend Status: {getStatusText()}
          </span>
          <button
            onClick={checkConnection}
            disabled={isChecking}
            className="text-xs underline hover:no-underline disabled:opacity-50"
          >
            Refresh
          </button>
        </div>
        <div className="text-xs mt-1 opacity-75">
          API: http://localhost:8080/api/v1
        </div>
      </div>
    </div>
  );
}
```

### **Step 3: Build and Start Both Systems**

#### 3.1 Backend Build & Start
```bash
# Terminal 1: Backend
cd /Users/sthwalonyoni/FIN

# Build the project
./gradlew build

# Start API server mode
java -jar app/build/libs/app.jar api

# You should see:
# 🚀 API Server started on http://localhost:8080
# 📊 Health check: http://localhost:8080/api/v1/health
# 🏢 Companies API: http://localhost:8080/api/v1/companies
```

#### 3.2 Frontend Start
```bash
# Terminal 2: Frontend
cd /path/to/drimacc

# Install dependencies (first time only)
npm install

# Start development server
npm run dev

# You should see:
# VITE v4.4.9  ready in 1234 ms
# ➜  Local:   http://localhost:3000/
# ➜  Network: use --host to expose
```

#### 3.3 Test Connection
```bash
# Terminal 3: Test API endpoints
curl http://localhost:8080/api/v1/health
# Should return: {"status":"OK","timestamp":1234567890}

curl http://localhost:8080/api/v1/companies
# Should return: [] or existing companies
```

## 🌐 **Access Points**

Once both systems are running:

- **Frontend UI:** http://localhost:3000
- **Backend API:** http://localhost:8080/api/v1
- **Health Check:** http://localhost:8080/api/v1/health
- **Companies API:** http://localhost:8080/api/v1/companies

## 🔧 **Development Workflow**

### **Daily Development:**
```bash
# Terminal 1: Start backend
cd /Users/sthwalonyoni/FIN && java -jar app/build/libs/app.jar api

# Terminal 2: Start frontend  
cd /path/to/drimacc && npm run dev

# Terminal 3: Available for testing/commands
curl http://localhost:8080/api/v1/health
```

### **Hot Reload Benefits:**
- ✅ **Frontend changes** - Instant reload via Vite
- ✅ **Backend changes** - Rebuild and restart API server
- ✅ **Real-time testing** - Both systems update independently

## 🐛 **Troubleshooting**

### **Backend Issues:**
```bash
# Check if port 8080 is in use
lsof -i :8080

# Kill process on port 8080 if needed
kill -9 $(lsof -t -i:8080)

# Check Java version
java -version  # Should be 17+
```

### **Frontend Issues:**
```bash
# Check if port 3000 is in use
lsof -i :3000

# Clear npm cache if needed
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json && npm install
```

### **Connection Issues:**
```bash
# Test backend directly
curl -v http://localhost:8080/api/v1/health

# Check CORS headers
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:8080/api/v1/companies
```

## 📊 **Development Scripts**

### **Create start script for easy development:**

#### start-full-stack.sh
```bash
#!/bin/bash
echo "🚀 Starting Full-Stack FIN System..."

# Start backend in background
echo "📊 Starting Java Backend..."
cd /Users/sthwalonyoni/FIN
java -jar app/build/libs/app.jar api &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 5

# Test backend
if curl -s http://localhost:8080/api/v1/health > /dev/null; then
    echo "✅ Backend started successfully"
else
    echo "❌ Backend failed to start"
    kill $BACKEND_PID
    exit 1
fi

# Start frontend
echo "🌐 Starting TypeScript Frontend..."
cd /path/to/drimacc
npm run dev &
FRONTEND_PID=$!

echo "🎉 Both systems are starting!"
echo "📊 Backend: http://localhost:8080/api/v1"
echo "🌐 Frontend: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop both systems"

# Wait for Ctrl+C
trap 'kill $BACKEND_PID $FRONTEND_PID; exit' INT
wait
```

#### Make it executable:
```bash
chmod +x start-full-stack.sh
./start-full-stack.sh
```

## 🎯 **Next Steps**

### **Immediate (Today):**
1. ✅ **Build backend API endpoints** (add ApiServer class)
2. ✅ **Test API connection** from frontend
3. ✅ **Verify CORS configuration** works

### **This Week:**
1. **Expand API endpoints** for full CRUD operations
2. **Connect frontend components** to real backend data
3. **Test file upload** (CSV/PDF processing)

### **Advanced Features:**
1. **Real-time updates** with WebSockets
2. **Authentication** and user management
3. **Production deployment** with Docker

---

**🚀 You're now ready to run both systems simultaneously!**

Your full-stack financial management platform will have:
- **Java backend** serving robust financial APIs
- **TypeScript frontend** providing beautiful user interface
- **Real-time connection** between both systems
- **Hot reload development** for rapid iteration

**Start with the Quick Start section above and you'll have both systems running in 5 minutes!** 🎉
