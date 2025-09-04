# 🚀 Quick Start Commands for Running Both Systems Simultaneously

## 🎯 **IMMEDIATE ACTIONS** - Run these commands now:

### **Terminal 1: Start Java Backend**
```bash
cd /Users/sthwalonyoni/FIN
./start-backend.sh
```

This will:
- ✅ Start your Java backend API on port 8080
- ✅ Test the connection automatically  
- ✅ Show all available endpoints
- ✅ Keep running with live logs

### **Terminal 2: Test API (while backend is running)**
```bash
cd /Users/sthwalonyoni/FIN
./test-api.sh
```

Expected output:
```json
{
  "status": "OK",
  "timestamp": 1234567890,
  "version": "1.0.0",
  "service": "FIN Financial Management API",
  "database": "connected",
  "companies_count": 0
}
```

### **Terminal 3: Start TypeScript Frontend (when ready)**
```bash
# Navigate to your drimacc frontend project
cd /path/to/drimacc

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

---

## 🌐 **Access Points** (once both are running)

- **Backend API:** http://localhost:8080/api/v1
- **API Health Check:** http://localhost:8080/api/v1/health  
- **API Documentation:** http://localhost:8080/api/v1/docs
- **Companies API:** http://localhost:8080/api/v1/companies
- **Frontend UI:** http://localhost:3000 (when started)

---

## 🔧 **Development Workflow**

### **Daily Development:**
1. **Start Backend:** `./start-backend.sh` in Terminal 1
2. **Test APIs:** `./test-api.sh` in Terminal 2  
3. **Start Frontend:** `npm run dev` in Terminal 3
4. **Develop:** Make changes to either system - both have hot reload

### **API Testing Commands:**
```bash
# Test health
curl http://localhost:8080/api/v1/health

# List companies
curl http://localhost:8080/api/v1/companies

# Create a company
curl -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Company", "contactEmail": "test@example.com"}'

# Test CORS for frontend
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     http://localhost:8080/api/v1/companies
```

---

## 🚨 **If Something Goes Wrong**

### **Backend Issues:**
```bash
# Check if port 8080 is in use
lsof -i :8080

# Kill any process on port 8080
kill -9 $(lsof -t -i:8080)

# Restart backend
./start-backend.sh
```

### **Frontend Issues:**
```bash
# Check if port 3000 is in use
lsof -i :3000

# Clear npm cache
npm cache clean --force

# Reinstall dependencies
rm -rf node_modules package-lock.json && npm install
```

### **Connection Issues:**
```bash
# Test backend directly
curl -v http://localhost:8080/api/v1/health

# Check backend logs in Terminal 1
# Look for error messages or CORS issues
```

---

## 🎉 **SUCCESS INDICATORS**

### **Backend Started Successfully:**
- ✅ See "Backend API Server started successfully!" message
- ✅ Health check returns JSON with "status": "OK"  
- ✅ Companies endpoint returns `{"success": true, "data": [], "count": 0}`

### **Frontend Connected Successfully:**
- ✅ Frontend loads at http://localhost:3000
- ✅ No CORS errors in browser console
- ✅ API calls work from frontend components

### **Full Integration Working:**
- ✅ Can create companies via API
- ✅ Frontend shows real backend data
- ✅ File uploads work (when implemented)
- ✅ No network errors in browser DevTools

---

## 🚀 **YOU'RE READY!**

**Start with Terminal 1** and run `./start-backend.sh` - everything else builds from there!

Your full-stack financial management system will have:
- **Java Backend:** Robust financial APIs with database
- **TypeScript Frontend:** Beautiful React interface  
- **Real-time Connection:** Live data flow between systems
- **Development Tools:** Hot reload, API testing, error handling

**The backend is ready to run RIGHT NOW!** 🎯
