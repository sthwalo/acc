#!/bin/bash

# FIN Container Startup Script
# Launches backend containers and local frontend, then opens browser

set -e

echo "🚀 Starting FIN Financial Management System..."

# Function to check if a port is open
wait_for_port() {
    local port=$1
    local service=$2
    local max_attempts=30
    local attempt=1

    echo "⏳ Waiting for $service on port $port..."
    while ! nc -z localhost $port 2>/dev/null; do
        if [ $attempt -ge $max_attempts ]; then
            echo "❌ Timeout waiting for $service on port $port"
            exit 1
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "✅ $service is ready on port $port"
}

# Start backend containers only
echo "🐳 Starting backend containers..."
docker compose up -d postgres fin-app

# Wait for backend to be ready
wait_for_port 8080 "FIN Backend API"

# Start frontend locally (faster than container build)
echo "⚛️  Starting frontend development server..."
cd frontend
npm run dev > /dev/null 2>&1 &
FRONTEND_PID=$!

# Wait for frontend to be ready
wait_for_port 3000 "FIN Frontend"

echo ""
echo "🎉 FIN System is ready!"
echo "📱 Frontend: http://localhost:3000"
echo "🔧 Backend API: http://localhost:8080"
echo ""

# Open browser (works on macOS, Linux, and WSL)
if command -v open >/dev/null 2>&1; then
    # macOS
    open http://localhost:3000
elif command -v xdg-open >/dev/null 2>&1; then
    # Linux
    xdg-open http://localhost:3000
elif command -v start >/dev/null 2>&1; then
    # WSL/Windows
    start http://localhost:3000
else
    echo "🌐 Please open your browser and navigate to: http://localhost:3000"
fi

echo ""
echo "💡 Useful commands:"
echo "  • View backend logs: docker compose logs -f"
echo "  • Stop containers: docker compose down"
echo "  • Stop frontend: kill $FRONTEND_PID"
echo ""

# Keep script running to maintain processes
wait $FRONTEND_PID