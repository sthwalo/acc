#!/bin/bash

echo "🚀 Starting FIN Full-Stack Development Environment"
echo "=================================================="

# Check if both ports are available
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo "❌ Port 8080 is already in use. Stopping existing process..."
    kill -9 $(lsof -t -i:8080) 2>/dev/null || true
    sleep 2
fi

if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null ; then
    echo "❌ Port 3000 is already in use. Please stop the frontend manually if needed."
fi

# Start the Java backend API server
echo "📊 Starting Java Backend API Server on port 8080..."
cd /Users/sthwalonyoni/FIN
java -jar app/build/libs/app.jar api &
BACKEND_PID=$!

# Wait a moment for backend to start
echo "⏳ Waiting for backend to start..."
sleep 5

# Test backend health
echo "🔍 Testing backend connection..."
if curl -s http://localhost:8080/api/v1/health > /dev/null; then
    echo "✅ Backend API Server started successfully!"
    echo "📊 Health Check: http://localhost:8080/api/v1/health"
    echo "🏢 Companies API: http://localhost:8080/api/v1/companies"
    echo "📖 API Docs: http://localhost:8080/api/v1/docs"
else
    echo "❌ Backend failed to start properly"
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
fi

echo ""
echo "🎯 Next Steps:"
echo "1. Backend is running on: http://localhost:8080/api/v1"
echo "2. Test API endpoints with: ./test-api.sh"
echo "3. Start your TypeScript frontend on port 3000"
echo "4. Visit frontend at: http://localhost:3000"
echo ""
echo "💡 Backend PID: $BACKEND_PID"
echo "🛑 To stop backend: kill $BACKEND_PID"
echo "🔄 To stop all: pkill -f 'app.jar api'"
echo ""
echo "🎉 Ready for full-stack development!"

# Keep the script running to show backend logs
wait $BACKEND_PID
