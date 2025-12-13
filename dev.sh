#!/bin/bash

# ===========================================
# FIN Financial Management System - Development Script
# Starts both frontend and backend simultaneously for local development
# ===========================================

echo "🚀 Starting FIN Development Environment..."
echo "=========================================="

# Load environment variables from .env file
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env..."
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        [[ $key =~ ^[[:space:]]*# ]] && continue
        [[ -z $key ]] && continue
        # Remove quotes from value if present
        value=$(echo "$value" | sed 's/^"\(.*\)"$/\1/' | sed "s/^'\(.*\)'$/\1/")
        export "$key=$value"
    done < .env
else
    echo "⚠️  Warning: .env file not found!"
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

# Start backend server in background
echo "📊 Starting Spring Boot backend server..."
cd app
./gradlew bootRun --no-daemon &
BACKEND_PID=$!

# Wait a moment for backend to start
sleep 5

# Start frontend development server in background
echo "🌐 Starting React frontend development server..."
cd ..
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