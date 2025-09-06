#!/bin/bash

# Full-Stack FIN Development Environment
# Runs both Java backend (port 8080) and TypeScript frontend (port 3000) simultaneously
# Copyright 2025 Immaculate Nyoni

set -e

echo "🏠 FIN Full-Stack Development Environment"
echo "=========================================="
echo "🚀 Starting both backend and frontend locally..."
echo ""

# Configuration
BACKEND_PORT=8080
FRONTEND_PORT=3000
BACKEND_DIR="/Users/sthwalonyoni/FIN"
FRONTEND_DIR="/Users/sthwalonyoni/drimacc"  # Adjust this path to your actual drimacc directory

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Shutting down full-stack environment..."
    
    # Kill backend
    if [ ! -z "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "🔪 Stopping backend (PID: $BACKEND_PID)..."
        kill -TERM "$BACKEND_PID" 2>/dev/null || true
        sleep 2
        kill -KILL "$BACKEND_PID" 2>/dev/null || true
    fi
    
    # Kill frontend
    if [ ! -z "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo "🔪 Stopping frontend (PID: $FRONTEND_PID)..."
        kill -TERM "$FRONTEND_PID" 2>/dev/null || true
        sleep 2
        kill -KILL "$FRONTEND_PID" 2>/dev/null || true
    fi
    
    # Kill any remaining processes on our ports
    pkill -f "app.jar api" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    
    echo "✅ Environment stopped successfully"
    exit 0
}

# Setup cleanup on script exit
trap cleanup EXIT INT TERM

# Check if ports are available
echo "🔍 Checking port availability..."

if lsof -Pi :$BACKEND_PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "❌ Port $BACKEND_PORT is already in use. Stopping existing process..."
    kill -9 $(lsof -t -i:$BACKEND_PORT) 2>/dev/null || true
    sleep 2
fi

if lsof -Pi :$FRONTEND_PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "❌ Port $FRONTEND_PORT is already in use. Stopping existing process..."
    kill -9 $(lsof -t -i:$FRONTEND_PORT) 2>/dev/null || true
    sleep 2
fi

echo "✅ Ports $BACKEND_PORT and $FRONTEND_PORT are available"
echo ""

# Check if backend directory exists
if [ ! -d "$BACKEND_DIR" ]; then
    echo "❌ Backend directory not found: $BACKEND_DIR"
    echo "📍 Please update BACKEND_DIR in this script to point to your FIN directory"
    exit 1
fi

# Check if backend JAR exists
if [ ! -f "$BACKEND_DIR/app/build/libs/app.jar" ]; then
    echo "❌ Backend JAR not found. Building backend..."
    cd "$BACKEND_DIR"
    ./gradlew build
    if [ ! -f "$BACKEND_DIR/app/build/libs/app.jar" ]; then
        echo "❌ Failed to build backend JAR"
        exit 1
    fi
fi

# Check if frontend directory exists
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "⚠️  Frontend directory not found: $FRONTEND_DIR"
    echo "📍 Please update FRONTEND_DIR in this script to point to your drimacc directory"
    echo "🔄 Continuing with backend only..."
    FRONTEND_AVAILABLE=false
else
    FRONTEND_AVAILABLE=true
fi

echo "=== STARTING BACKEND ==="
echo "📊 Starting Java Backend API Server on port $BACKEND_PORT..."
cd "$BACKEND_DIR"

# Start backend in background
java -jar app/build/libs/app.jar api > backend.log 2>&1 &
BACKEND_PID=$!

echo "⏳ Waiting for backend to start..."
sleep 8

# Test backend health
echo "🔍 Testing backend connection..."
BACKEND_HEALTH_ATTEMPTS=0
BACKEND_READY=false

while [ $BACKEND_HEALTH_ATTEMPTS -lt 10 ]; do
    if curl -s http://localhost:$BACKEND_PORT/api/v1/health > /dev/null; then
        BACKEND_READY=true
        break
    fi
    echo "⏳ Backend not ready yet, waiting... (attempt $((BACKEND_HEALTH_ATTEMPTS + 1))/10)"
    sleep 3
    BACKEND_HEALTH_ATTEMPTS=$((BACKEND_HEALTH_ATTEMPTS + 1))
done

if [ "$BACKEND_READY" = true ]; then
    echo "✅ Backend API Server started successfully!"
    echo "📊 Health Check: http://localhost:$BACKEND_PORT/api/v1/health"
    echo "🏢 Companies API: http://localhost:$BACKEND_PORT/api/v1/companies"
    echo "📖 API Documentation: http://localhost:$BACKEND_PORT/api/v1/docs"
else
    echo "❌ Backend failed to start properly"
    echo "📋 Backend log:"
    cat backend.log
    exit 1
fi

echo ""

# Start frontend if available
if [ "$FRONTEND_AVAILABLE" = true ]; then
    echo "=== STARTING FRONTEND ==="
    echo "🎨 Starting TypeScript Frontend on port $FRONTEND_PORT..."
    cd "$FRONTEND_DIR"
    
    # Check if package.json exists
    if [ ! -f "package.json" ]; then
        echo "❌ Frontend package.json not found in $FRONTEND_DIR"
        echo "📍 Please ensure you're pointing to the correct drimacc directory"
        echo "🔄 Continuing with backend only..."
    else
        # Check if node_modules exists
        if [ ! -d "node_modules" ]; then
            echo "📦 Installing frontend dependencies..."
            npm install
        fi
        
        # Start frontend in background
        npm run dev > ../frontend.log 2>&1 &
        FRONTEND_PID=$!
        
        echo "⏳ Waiting for frontend to start..."
        sleep 8
        
        # Test frontend
        FRONTEND_HEALTH_ATTEMPTS=0
        FRONTEND_READY=false
        
        while [ $FRONTEND_HEALTH_ATTEMPTS -lt 10 ]; do
            if curl -s http://localhost:$FRONTEND_PORT > /dev/null; then
                FRONTEND_READY=true
                break
            fi
            echo "⏳ Frontend not ready yet, waiting... (attempt $((FRONTEND_HEALTH_ATTEMPTS + 1))/10)"
            sleep 3
            FRONTEND_HEALTH_ATTEMPTS=$((FRONTEND_HEALTH_ATTEMPTS + 1))
        done
        
        if [ "$FRONTEND_READY" = true ]; then
            echo "✅ Frontend started successfully!"
            echo "🌐 Frontend URL: http://localhost:$FRONTEND_PORT"
        else
            echo "⚠️  Frontend may still be starting up"
            echo "📋 Check frontend log: $BACKEND_DIR/frontend.log"
        fi
    fi
fi

echo ""
echo "🎉 FULL-STACK ENVIRONMENT READY!"
echo "=================================="
echo ""
echo "🔗 Services Running:"
echo "   📊 Backend API:  http://localhost:$BACKEND_PORT"
echo "   🎨 Frontend UI:  http://localhost:$FRONTEND_PORT"
echo ""
echo "🛠️  Development URLs:"
echo "   🏥 Health Check: http://localhost:$BACKEND_PORT/api/v1/health"
echo "   🏢 Companies:    http://localhost:$BACKEND_PORT/api/v1/companies"
echo "   📖 API Docs:     http://localhost:$BACKEND_PORT/api/v1/docs"
echo ""
echo "💻 Process IDs:"
echo "   📊 Backend PID:  $BACKEND_PID"
if [ ! -z "$FRONTEND_PID" ]; then
    echo "   🎨 Frontend PID: $FRONTEND_PID"
fi
echo ""
echo "📋 Log Files:"
echo "   📊 Backend:  $BACKEND_DIR/backend.log"
if [ "$FRONTEND_AVAILABLE" = true ]; then
    echo "   🎨 Frontend: $BACKEND_DIR/frontend.log"
fi
echo ""
echo "🔧 Quick Commands:"
echo "   📊 Test Backend:  curl http://localhost:$BACKEND_PORT/api/v1/health"
echo "   🏢 List Companies: curl http://localhost:$BACKEND_PORT/api/v1/companies"
echo ""
echo "🛑 Press Ctrl+C to stop all services"
echo ""

# Keep script running and show real-time logs
echo "📋 Live Backend Logs (Press Ctrl+C to stop):"
echo "=============================================="
tail -f "$BACKEND_DIR/backend.log"
