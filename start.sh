#!/bin/bash

# FIN Development Workflow Script
# Builds applications locally and runs them in Docker containers
# Much faster than building inside containers!

set -e

echo "🚀 Starting FIN Development Environment..."
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required tools are installed
check_dependencies() {
    print_status "Checking dependencies..."

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17+"
        exit 1
    fi

    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js 20+"
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install npm"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker"
        exit 1
    fi

    print_success "All dependencies are installed"
}

# Build backend
build_backend() {
    print_status "Building Spring Boot backend..."
    cd spring-app

    if [ ! -f "build/libs/fin-spring.jar" ] || [ "build.gradle.kts" -nt "build/libs/fin-spring.jar" ]; then
        print_status "Building JAR file..."
        ./gradlew clean build --no-daemon -x test
        print_success "Backend built successfully"
    else
        print_warning "JAR file is up to date, skipping build"
    fi

    cd ..
}

# Build frontend
build_frontend() {
    print_status "Building React frontend..."
    cd frontend

    if [ ! -d "dist" ] || find src -newer dist -type f | read; then
        print_status "Building frontend dist..."
        npm install
        # Set API URL for containerized frontend to reach backend via host port
        export VITE_API_URL=http://localhost:8080/api
        npm run build
        print_success "Frontend built successfully"
    else
        print_warning "Frontend dist is up to date, skipping build"
    fi

    cd ..
}

# Start containers
start_containers() {
    print_status "Starting Docker containers..."

    # Stop any existing containers
    docker compose -f docker-compose.yml -f docker-compose.frontend.yml down 2>/dev/null || true

    # Start containers
    docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

    print_success "Containers started successfully"
}

# Wait for services to be ready
wait_for_services() {
    print_status "Waiting for services to be ready..."

    # Wait for backend
    print_status "Waiting for backend (port 8080)..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
            print_success "Backend is ready!"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done

    if [ $timeout -le 0 ]; then
        print_error "Backend failed to start within 60 seconds"
        exit 1
    fi

    # Wait for frontend
    print_status "Waiting for frontend (port 3000)..."
    timeout=30
    while [ $timeout -gt 0 ]; do
        if curl -s -I http://localhost:3000 | grep -q "200 OK"; then
            print_success "Frontend is ready!"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done

    if [ $timeout -le 0 ]; then
        print_warning "Frontend health check timed out, but service may still be starting"
    fi
}

# Main execution
main() {
    check_dependencies
    build_backend
    build_frontend
    start_containers
    wait_for_services

    echo ""
    print_success "🎉 FIN Development Environment is ready!"
    echo ""
    echo "🌐 Frontend: http://localhost:3000"
    echo "🔧 Backend API: http://localhost:8080"
    echo "📊 API Health: http://localhost:8080/api/v1/health"
    echo ""
    echo "To stop the environment, run:"
    echo "  docker compose -f docker-compose.yml -f docker-compose.frontend.yml down"
    echo ""
    echo "To rebuild and restart:"
    echo "  $0"
}

# Run main function
main "$@"