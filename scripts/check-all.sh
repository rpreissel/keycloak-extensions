#!/bin/bash

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     🚀 Keycloak Development Environment - Status         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if containers are running
echo "📦 Container Status:"
podman ps --format "   ✅ {{.Names}}: {{.Status}}" | grep -E "(keycloak|postgres|test-client|reverse-proxy|mock-idp)"
echo ""

# Check Nginx
echo "🌐 Reverse Proxy (Nginx):"
if curl -s http://localhost/health > /dev/null 2>&1; then
    echo "   ✅ http://localhost (Port 80) - Healthy"
else
    echo "   ❌ http://localhost (Port 80) - Not responding"
fi
echo ""

# Service URLs
echo "🔗 Service URLs (via Nginx Reverse Proxy):"
echo "   🌐 Web Test Client:    http://localhost/app"
echo "   🔐 Keycloak Admin:     http://localhost/admin"
echo "   🎯 Keycloak Realms:    http://localhost/realms/test-realm"
echo "   🧪 Mock IDP:           http://localhost/mock-idp"
echo ""

# Direct Access (für Entwicklung)
echo "🔌 Direct Container Access:"
echo "   Keycloak:    http://localhost:8081"
echo "   PostgreSQL:  localhost:5432"
echo "   Debug Port:  localhost:5005"
echo "   Management:  http://localhost:9000"
echo ""

# Credentials
echo "🔑 Credentials:"
echo "   Keycloak Admin:  admin / admin"
echo "   Test User:       testuser / test123"
echo ""

# Quick Tests
echo "🧪 Quick Health Checks:"
if curl -s http://localhost/health | grep -q "healthy"; then
    echo "   ✅ Nginx:        Healthy"
else
    echo "   ❌ Nginx:        Failed"
fi

if curl -s http://localhost/app/health | grep -q "ok"; then
    echo "   ✅ Test Client:  Healthy"
else
    echo "   ❌ Test Client:  Failed"
fi

if curl -s http://localhost:9000/health/ready | grep -q "UP"; then
    echo "   ✅ Keycloak:     Ready"
else
    echo "   ⏳ Keycloak:     Starting..."
fi
echo ""

# Next Steps
echo "📝 Next Steps:"
echo "   1. Open http://localhost/app in your browser"
echo "   2. Click 'Mit Keycloak anmelden'"
echo "   3. Login with testuser / test123"
echo "   4. View Access Token and ID Token as JSON"
echo ""

echo "💡 Useful Commands:"
echo "   View logs:       ./scripts/logs.sh"
echo "   Stop all:        ./scripts/stop-keycloak.sh"
echo "   Restart:         ./scripts/restart-keycloak.sh"
echo "   Build & Deploy:  ./scripts/build-deploy.sh"
echo ""
