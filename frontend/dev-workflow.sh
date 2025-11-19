#!/bin/bash
# Frontend Development Workflow Script
# Implements container-first development as per copilot-instructions.md

set -e

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

# Check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Start containerized backend
start_backend() {
    print_status "Starting containerized backend..."
    docker-compose up -d fin-app postgres

    # Wait for backend to be healthy
    print_status "Waiting for backend to be ready..."
    max_attempts=30
    attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:8080/health >/dev/null 2>&1; then
            print_success "Backend is ready!"
            return 0
        fi

        print_status "Waiting for backend... (attempt $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    print_error "Backend failed to start within expected time"
    exit 1
}

# Start frontend development server
start_frontend() {
    print_status "Starting frontend development server..."

    # Check if port 3000 is available
    if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "Port 3000 is already in use. Attempting to use it anyway..."
    fi

    # Start frontend with container proxy configuration
    npm run dev
}

# Start full containerized development environment
start_full_container() {
    print_status "Starting full containerized development environment..."
    docker-compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

    print_success "Containerized environment started!"
    print_status "Frontend: http://localhost:3000"
    print_status "Backend API: http://localhost:8080"
    print_status "PostgreSQL: localhost:5432"
}

# Stop all containers
stop_containers() {
    print_status "Stopping all containers..."
    docker-compose -f docker-compose.yml -f docker-compose.frontend.yml down
    print_success "Containers stopped"
}

# Show usage
usage() {
    echo "Frontend Development Workflow Script"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  dev          Start frontend dev server (expects backend running)"
    echo "  backend      Start containerized backend only"
    echo "  full         Start full containerized environment (backend + frontend)"
    echo "  stop         Stop all containers"
    echo "  build        Build frontend for production"
    echo "  test         Run frontend tests"
    echo "  lint         Run linting"
    echo "  clean        Clean build artifacts"
    echo ""
    echo "Examples:"
    echo "  $0 backend    # Start backend in container"
    echo "  $0 dev        # Start frontend dev server"
    echo "  $0 full       # Start everything in containers"
}

# Main script logic
case "${1:-dev}" in
    "backend")
        check_docker
        start_backend
        print_success "Backend started. Run '$0 dev' to start frontend."
        ;;
    "dev")
        check_docker
        # Check if backend is running
        if ! curl -f http://localhost:8080/health >/dev/null 2>&1; then
            print_warning "Backend is not running. Starting it automatically..."
            start_backend
        fi
        start_frontend
        ;;
    "full")
        check_docker
        start_full_container
        ;;
    "stop")
        stop_containers
        ;;
    "build")
        print_status "Building frontend for production..."
        npm run build:verify
        print_success "Build completed successfully"
        ;;
    "test")
        print_status "Running tests..."
        npm test
        ;;
    "lint")
        print_status "Running linter..."
        npm run lint
        ;;
    "clean")
        print_status "Cleaning build artifacts..."
        npm run clean
        print_success "Clean completed"
        ;;
    "help"|"-h"|"--help")
        usage
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        usage
        exit 1
        ;;
esac