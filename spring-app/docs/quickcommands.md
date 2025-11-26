# ===========================================
# FIN Financial Management System - Quick Commands
# Organized by execution order with comments
# ===========================================

# 🚀 FAST DEVELOPMENT WORKFLOW OVERVIEW
# ===========================================
# FIN uses Docker containers for production-same environment, but builds locally for speed.
# This gives you the best of both worlds: production accuracy + fast development iterations.
#
# WORKFLOW:
# 1. Make code changes in VSCode
# 2. Run: ./start.sh (builds locally in seconds, starts containers)
# 3. Test your changes against real containers
# 4. Iterate quickly - no waiting for Docker builds!
#
# WHY THIS WORKS:
# - ✅ Production-same environment (container networking, same JVM, dependencies)
# - ✅ Fast iterations (local builds in seconds vs Docker builds in minutes)
# - ✅ Zero deployment surprises (test against same runtime as production)
# - ✅ Frontend integration testing (real API calls in container network)
#
# KEY COMMANDS:
# - ./start.sh          → Build locally + start containers (RECOMMENDED)
# - npm run dev            → Frontend-only development
# - ./gradlew build        → Backend-only local build
# - docker compose ...     → Manage containers with pre-built artifacts

cd /Users/sthwalonyoni/FIN && docker compose -f docker-compose.yml -f docker-compose.frontend.yml down && cd spring-app && ./gradlew clean build --no-daemon && cd .. && docker compose -f docker-compose.yml build fin-app && docker compose -f docker-compose.yml up -d fin-app

# ===========================================
# 1. ENVIRONMENT SETUP
# ===========================================

# Navigate to project root
cd /Users/sthwalonyoni/FIN

# Navigate to frontend directory (for frontend development)
cd /Users/sthwalonyoni/FIN/frontend

# Navigate to backend directory (for backend development)
cd /Users/sthwalonyoni/FIN/spring-app

# ===========================================
# 2. DATABASE SETUP & CHECKS
# ===========================================

# Load environment variables
source .env

# Check database connection and version
source .env 2>/dev/null || echo "No .env file found" && echo "Checking database connection..." && psql -h localhost -U $DATABASE_USER -d $DATABASE_NAME -c "SELECT version();" 2>/dev/null || echo "Database connection failed - check .env file"

# Connect to PostgreSQL database
psql -h localhost -U sthwalonyoni -d drimacc_db

# List all tables in database
psql -h localhost -d drimacc_db -U sthwalonyoni -c "\dt"

# View all users
source .env && psql -U $DATABASE_USER -d drimacc_db -c "SELECT id, email, first_name, last_name, is_active, created_at FROM users;"

# View specific user password hash
source .env && psql -U $DATABASE_USER -d drimacc_db -c "SELECT id, email, password_hash, salt FROM users WHERE email = 'sthwaloe@gmail.com';"

# Update user password (bcrypt hash)
source .env && psql -U $DATABASE_USER -d drimacc_db -c "UPDATE users SET password_hash = '\$2a\$10\$bXOqemA63tpn0OrXpFhkAOL0m29vjzVHbVhOK7U9PGi40asMUaTh6' WHERE email = 'sthwaloe@gmail.com';"

# Restore database from backup
source .env && psql -U $DATABASE_USER -d drimacc_db < backups/drimacc_db_backup_2025-11-10_020000.dump

# ===========================================
# 3. BUILD COMMANDS
# ===========================================

# Build backend JAR only (local build - FAST)
cd /Users/sthwalonyoni/FIN/spring-app && ./gradlew clean build

# Build frontend dist only (local build - FAST)
cd /Users/sthwalonyoni/FIN/frontend && npm install && npm run build

# Build both applications locally (RECOMMENDED - Fast development)
cd /Users/sthwalonyoni/FIN && ./start.sh

# Legacy: Build backend Docker image only (slow - not recommended)
cd /Users/sthwalonyoni/FIN/spring-app && ./gradlew build && docker build -f ../Dockerfile.spring-prebuilt -t fin-fin-app .

# Legacy: Build both backend and frontend Docker images (slow - not recommended)
docker compose -f docker-compose.yml -f docker-compose.frontend.yml build --no-cache

# ===========================================
# 4. START/RUN COMMANDS
# ===========================================

# FAST DEVELOPMENT WORKFLOW (RECOMMENDED) - Build locally, run in containers
cd /Users/sthwalonyoni/FIN && ./start.sh

docker compose -f docker-compose.yml -f docker-compose.frontend.yml restart fin-app

cd /Users/sthwalonyoni/FIN/spring-app && rm build/libs/fin-spring.jar && ./gradlew clean build --no-daemon -x test && cd .. && docker compose -f docker-compose.yml -f docker-compose.frontend.yml down && docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# Start frontend development server (local development - for frontend-only work)
cd /Users/sthwalonyoni/FIN/frontend && npm run build

# Run backend JAR directly (development/testing - bypasses containers)
java -jar spring-app/build/libs/fin-spring.jar

# Start backend container only (uses pre-built JAR - FAST)
docker compose -f docker-compose.yml build fin-app && docker compose -f docker-compose.yml up -d fin-app

# Re-start backend container only
docker compose -f docker-compose.yml restart fin-app
docker compose restart fin-app

# Start both backend and frontend containers (uses pre-built artifacts - FAST)
docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# Legacy: Start both with rebuild inside containers (SLOW - not recommended)
docker compose -f docker-compose.yml -f docker-compose.frontend.yml up --build -d

# ===========================================
# 5. TEST & DEBUG COMMANDS
# ===========================================

# Check container status
docker compose -f docker-compose.yml -f docker-compose.frontend.yml ps

# Check running processes (frontend)
ps aux | grep -E "(vite|node|npm)" | grep -v grep

# Check running processes (backend)
ps aux | grep -E "(java|node)" | grep -v grep | grep -E "(vite|spring-app)"

# Test backend health endpoint
curl -s http://localhost:8080/api/v1/health | jq .

# Test authentication login (FIXED: AuthContext no longer clears tokens on app load)
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"sthwaloe@gmail.com","password":"password"}' | jq .

# Test authentication with different user
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"test@example.com","password":"password"}' | jq .

# Test API with JWT token (fiscal periods) - Replace YOUR_JWT_TOKEN with actual token
curl -s -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8080/api/v1/companies/2/fiscal-periods | jq .

# Upload bank statement file
curl -s "http://localhost:8080/api/v1/companies/2/fiscal-periods/8/imports/bank-statement" -F 'file=@"/Users/sthwalonyoni/FIN/input/GHC:FNB/GOLD BUSINESS ACCOUNT 111.pdf"' | jq .

# ===========================================
# 6. CLEANUP & MAINTENANCE
# ===========================================

# Stop and remove specific container
docker stop fin-app && docker rm fin-app

# Stop all containers
docker compose -f docker-compose.yml -f docker-compose.frontend.yml down

# ===========================================
# QUICK WORKFLOWS
# ===========================================

# 🚀 FAST DEVELOPMENT WORKFLOW (RECOMMENDED - Build locally, deploy to containers)
# Builds both apps locally in seconds, then starts containers with pre-built artifacts
cd /Users/sthwalonyoni/FIN && ./start.sh

# Backend-only development (RECOMMENDED: backend in container, frontend local)
# Build backend locally, run in container, develop frontend locally against container API
cd /Users/sthwalonyoni/FIN/spring-app && ./gradlew build && docker compose -f ../docker-compose.yml up -d fin-app && cd ../frontend && npm run dev

# Frontend-only development (develop against running backend container)
cd /Users/sthwalonyoni/FIN/frontend && npm run dev

# Legacy workflows (SLOW - builds inside containers, takes several minutes)
# Complete development setup (build and run both services in containers)
cd /Users/sthwalonyoni/FIN && docker compose -f docker-compose.yml -f docker-compose.frontend.yml build --no-cache && docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# Full rebuild and restart (legacy - slow)
cd /Users/sthwalonyoni/FIN && docker compose -f docker-compose.yml -f docker-compose.frontend.yml down && docker compose -f docker-compose.yml -f docker-compose.frontend.yml build --no-cache && docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

cd /Users/sthwalonyoni/FIN/spring-app && rm build/libs/fin-spring.jar && ./gradlew clean build --no-daemon -x test && cd .. && docker compose -f docker-compose.yml -f docker-compose.frontend.yml down && docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d