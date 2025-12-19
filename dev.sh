#!/bin/bash

# ===========================================
# FIN Financial Management System - Development Script
# Starts both frontend and backend simultaneously for local development
# ===========================================

echo "🚀 Starting FIN Development Environment..."
echo "=========================================="

# Load environment variables from .env file (supports both 'KEY=val' and 'export KEY=val')
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env..."
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        [[ $key =~ ^[[:space:]]*# ]] && continue
        [[ -z $key ]] && continue
        # Remove leading 'export ' and trailing whitespace from key
        key=$(echo "$key" | sed -E 's/^[[:space:]]*export[[:space:]]*//; s/[[:space:]]*$//')
        # Remove surrounding quotes from value if present
        value=$(echo "$value" | sed 's/^"\(.*\)"$/\1/' | sed "s/^'\(.*\)'$/\1/")
        # Validate key is a shell identifier
        if [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
            export "$key=$value"
        else
            echo "⚠️ Skipping invalid env line: '$key'"
        fi
    done < .env
else
    echo "⚠️  Warning: .env file not found!"
fi

# Validate required environment variables early to fail-fast with a clear message
missing=""
for var in JWT_SECRET DATABASE_URL DATABASE_USER DATABASE_PASSWORD; do
    if [ -z "${!var}" ]; then
        missing="$missing $var"
    fi
done
if [ -n "$missing" ]; then
    echo "❌ Missing required environment variable(s):$missing"
    echo "Please set them in your environment or in .env (use KEY=VALUE lines, not 'export KEY=VALUE') and retry."
    echo "Example (temporary): export JWT_SECRET=$(openssl rand -hex 32) && ./dev.sh"
    exit 1
fi

# Function to handle cleanup on script exit
cleanup() {
    echo ""
    echo "🛑 Stopping development servers..."
    # Kill any background processes started by this script
    kill $(jobs -p) 2>/dev/null
    exit
}

# Set up trap to cleanup on script exit
trap cleanup EXIT INT TERM

# Start backend server in background (run from repo root so settings and version catalogs are available)
echo "📊 Starting Spring Boot backend server..."
mkdir -p logs
./gradlew :app:bootRun --no-daemon > logs/backend.log 2>&1 &
BACKEND_PID=$!

# Wait for backend health endpoint with timeout
echo "⏳ Waiting for backend to become healthy (up to 30s)..."
timeout=30
while [ $timeout -gt 0 ]; do
    if curl -s http://localhost:8080/api/v1/health >/dev/null 2>&1; then
        echo "✅ Backend is healthy!"
        break
    fi
    # Check if the backend process has exited early
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo "❌ Backend process exited unexpectedly. See logs: logs/backend.log"
        tail -n 200 logs/backend.log
        exit 1
    fi
    sleep 2
    timeout=$((timeout - 2))
done
if [ $timeout -le 0 ]; then
    echo "⚠️ Backend did not become healthy within 30s; check logs: logs/backend.log"
fi

# Start frontend development server in background
echo "🌐 Starting React frontend development server..."
cd frontend
npm run dev &
FRONTEND_PID=$!

echo ""
echo "✅ Development servers started!"
echo "📊 Backend: http://localhost:8080"
echo "🌐 Frontend: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop both servers"

# Wait for both processes
wait $BACKEND_PID $FRONTEND_PID