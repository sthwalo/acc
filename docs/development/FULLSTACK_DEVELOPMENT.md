# 🚀 FIN Full-Stack Local Development Guide

## 🎯 Quick Start

### 1. Run Both Frontend and Backend Simultaneously

```bash
# Navigate to your FIN backend directory
cd /path/to/your/FIN

# Start both backend and frontend
./start-fullstack.sh
```

This will:
- ✅ Start Java backend API on **port 8080**
- ✅ Start TypeScript frontend on **port 3000**
- ✅ Configure CORS for frontend-backend communication
- ✅ Display real-time logs

### 2. Test the Full-Stack Setup

```bash
# Test all API endpoints and connectivity
./test-fullstack.sh
```

## 🔗 Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| 🎨 **Frontend UI** | http://localhost:3000 | React TypeScript interface |
| 📊 **Backend API** | http://localhost:8080/api/v1 | REST API endpoints |
| 🏥 **Health Check** | http://localhost:8080/api/v1/health | System status |
| 🏢 **Companies API** | http://localhost:8080/api/v1/companies | Company management |
| 📖 **API Documentation** | http://localhost:8080/api/v1/docs | API reference |

## 🛠️ Development Workflow

### Backend Development (Java)

```bash
# Build backend changes
./gradlew build

# Start backend only
java -jar app/build/libs/app.jar api

# Or use the existing script
./start-backend.sh
```

### Frontend Development (TypeScript)

```bash
# Navigate to frontend directory
cd /path/to/your/drimacc

# Install dependencies (first time)
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

### Full-Stack Development

```bash
# Start both services
./start-fullstack.sh

# In another terminal, test the setup
./test-fullstack.sh

# View logs in real-time
tail -f backend.log    # Backend logs
tail -f frontend.log   # Frontend logs (if available)
```

## 🔧 Configuration

### Backend Configuration

- **Port**: 8080
- **Database**: PostgreSQL (via environment variables)
- **CORS**: Enabled for `http://localhost:3000`
- **API Base**: `/api/v1`

### Frontend Configuration

- **Port**: 3000
- **Backend URL**: `http://localhost:8080/api/v1`
- **Tech Stack**: React + TypeScript + Vite

## 📋 Available API Endpoints

### Companies
- `GET /api/v1/companies` - List all companies
- `POST /api/v1/companies` - Create new company
- `GET /api/v1/companies/{id}/fiscal-periods` - Get fiscal periods
- `GET /api/v1/companies/{id}/transactions` - Get transactions

### System
- `GET /api/v1/health` - Health check
- `GET /api/v1/docs` - API documentation

### Example API Calls

```bash
# Health check
curl http://localhost:8080/api/v1/health

# List companies
curl http://localhost:8080/api/v1/companies

# Create a company
curl -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Company Ltd",
    "registrationNumber": "REG123",
    "taxNumber": "TAX456",
    "address": "123 Business St",
    "contactEmail": "contact@mycompany.com",
    "contactPhone": "+1234567890"
  }'
```

## 🐛 Troubleshooting

### Port Issues

```bash
# Kill processes on port 8080 (backend)
lsof -ti:8080 | xargs kill -9

# Kill processes on port 3000 (frontend)
lsof -ti:3000 | xargs kill -9

# Or use the comprehensive cleanup
pkill -f "app.jar api"
pkill -f "vite"
```

### Backend Issues

```bash
# Check if JAR is built
ls -la app/build/libs/app.jar

# Rebuild if needed
./gradlew clean build

# Check database
ls -la fin_database.db

# View backend logs
tail -f backend.log
```

### Frontend Issues

```bash
# Check if drimacc directory exists
ls -la /path/to/your/drimacc

# Install dependencies
cd /path/to/your/drimacc
npm install

# Check package.json scripts
cat package.json | grep -A 5 "scripts"
```

### CORS Issues

If frontend can't connect to backend:

1. Check browser console for CORS errors
2. Verify backend CORS configuration in `ApiServer.java`:
   ```java
   response.header("Access-Control-Allow-Origin", "http://localhost:3000");
   ```
3. Test CORS with curl:
   ```bash
   curl -H "Origin: http://localhost:3000" \
        -H "Access-Control-Request-Method: GET" \
        -X OPTIONS \
        http://localhost:8080/api/v1/health
   ```

## 🔄 Hot Reloading

### Backend Hot Reload

Backend requires restart after changes:
```bash
# Stop current backend
pkill -f "app.jar api"

# Rebuild and restart
./gradlew build && ./start-fullstack.sh
```

### Frontend Hot Reload

Frontend automatically reloads on file changes when using `npm run dev`.

## 🚀 Production Deployment

### Backend Production

```bash
# Build production JAR
./gradlew build

# Run with production profile
java -jar app/build/libs/app.jar api
```

### Frontend Production

```bash
cd /path/to/your/drimacc

# Build production bundle
npm run build

# Serve production build
npm run preview
```

## 📱 Mobile Development

For mobile testing:

1. Find your local IP: `ifconfig | grep inet`
2. Update CORS in backend to allow your IP
3. Access frontend via: `http://YOUR_IP:3000`

## 🔒 Security Notes

- Current setup is for **development only**
- Production requires proper authentication
- Database should use proper credentials
- CORS should be restricted in production
- HTTPS should be enabled for production

---

**🎉 Happy full-stack development!**

For issues or questions:
- 📧 Email: contact@fin-app.com
- 📖 Documentation: See `docs/` directory
- 🐛 Issues: Check logs and troubleshooting section above
