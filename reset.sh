#!/bin/bash

# FIN Full Container Reset Script
# Builds applications inside Docker containers and restarts everything
# Slower than local builds but ensures production-identical artifacts

set -e

echo "🔄 Starting FIN Full Container Reset..."
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

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker"
        exit 1
    fi

    print_success "All dependencies are installed"
}

# Build containers with no cache
build_containers() {
    print_status "Building Docker containers with no cache (this may take several minutes)..."
    docker compose -f docker-compose.yml -f docker-compose.frontend.yml build --no-cache
    print_success "Containers built successfully"
}

# Stop containers
stop_containers() {
    print_status "Stopping existing containers..."
    docker compose -f docker-compose.yml -f docker-compose.frontend.yml down
    print_success "Containers stopped"
}

# Start containers
start_containers() {
    print_status "Starting containers..."
    docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d
    print_success "Containers started"
}

# Wait for services to be ready
wait_for_services() {
    print_status "Waiting for services to be ready..."

    # Wait for backend
    print_status "Waiting for backend (port 8080)..."
    timeout=120  # Increased timeout for container builds
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
            print_success "Backend is ready!"
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done

    if [ $timeout -le 0 ]; then
        print_error "Backend failed to start within 2 minutes"
        exit 1
    fi

    # Wait for frontend
    print_status "Waiting for frontend (port 3000)..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s -I http://localhost:3000 | grep -q "200 OK"; then
            print_success "Frontend is ready!"
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done

    if [ $timeout -le 0 ]; then
        print_warning "Frontend health check timed out, but service may still be starting"
    fi
}

# Main execution
main() {
    check_dependencies
    build_containers
    stop_containers
    start_containers
    wait_for_services

    echo ""
    print_success "🎉 FIN Full Container Reset Complete!"
    echo ""
    echo "🌐 Frontend: http://localhost:3000"
    echo "🔧 Backend API: http://localhost:8080"
    echo "📊 API Health: http://localhost:8080/api/v1/health"
    echo ""
    echo "Note: This process builds applications inside containers,"
    echo "which takes longer but ensures production-identical artifacts."
    echo ""
    echo "For faster development iterations, use: ./start.sh"
}

# Run main function
main "$@"