#!/bin/bash

echo "🚀 Testing FIN API Server"
echo "=========================="

# Test health endpoint
echo "📊 Testing Health Endpoint:"
curl -s http://localhost:8080/api/v1/health | jq .

echo ""
echo "🏢 Testing Companies Endpoint:"
curl -s http://localhost:8080/api/v1/companies | jq .

echo ""
echo "📋 Testing API Documentation:"
curl -s http://localhost:8080/api/v1/docs | jq .

echo ""
echo "✅ API Tests Complete!"
