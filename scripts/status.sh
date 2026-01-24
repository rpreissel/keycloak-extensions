#!/bin/bash

# Load configuration
source "$(dirname "$0")/config.sh"

echo "📊 Keycloak Development Environment Status"
echo "==========================================="
echo ""

# Check Podman Machine
echo "🖥️  Podman Machine:"
if podman machine list | grep -q "Currently running"; then
    echo "   ✅ Running"
else
    echo "   ❌ Not running - Start with: podman machine start podman-machine-default"
fi
echo ""

# Check Containers
echo "🐳 Containers:"
if podman ps --format "{{.Names}}" | grep -q "keycloak-dev"; then
    echo "   ✅ Keycloak:  Running"
else
    echo "   ❌ Keycloak:  Not running"
fi

if podman ps --format "{{.Names}}" | grep -q "keycloak-postgres"; then
    echo "   ✅ PostgreSQL: Running"
else
    echo "   ❌ PostgreSQL: Not running"
fi
echo ""

# Check Keycloak Health
echo "🏥 Keycloak Health:"
if curl -sf http://localhost:${KEYCLOAK_MGMT_PORT}/health/ready > /dev/null 2>&1; then
    echo "   ✅ Ready"
else
    echo "   ❌ Not ready or not accessible"
fi
echo ""

# Check Ports
echo "🔌 Ports:"
if nc -z localhost ${KEYCLOAK_PORT} 2>/dev/null; then
    echo "   ✅ ${KEYCLOAK_PORT} (HTTP): Accessible"
else
    echo "   ❌ ${KEYCLOAK_PORT} (HTTP): Not accessible"
fi

if nc -z localhost ${KEYCLOAK_MGMT_PORT} 2>/dev/null; then
    echo "   ✅ ${KEYCLOAK_MGMT_PORT} (Management): Accessible"
else
    echo "   ❌ ${KEYCLOAK_MGMT_PORT} (Management): Not accessible"
fi

if nc -z localhost ${KEYCLOAK_DEBUG_PORT} 2>/dev/null; then
    echo "   ✅ ${KEYCLOAK_DEBUG_PORT} (Debug): Accessible"
else
    echo "   ❌ ${KEYCLOAK_DEBUG_PORT} (Debug): Not accessible"
fi

if nc -z localhost ${POSTGRES_PORT} 2>/dev/null; then
    echo "   ✅ ${POSTGRES_PORT} (PostgreSQL): Accessible"
else
    echo "   ❌ ${POSTGRES_PORT} (PostgreSQL): Not accessible"
fi
echo ""

# Check providers directory
echo "📦 Extensions:"
PROVIDER_COUNT=$(find providers -name "*.jar" 2>/dev/null | wc -l)
echo "   ${PROVIDER_COUNT} JAR file(s) in providers/"
if [ $PROVIDER_COUNT -gt 0 ]; then
    echo ""
    echo "   Files:"
    find providers -name "*.jar" -exec basename {} \; | sed 's/^/   - /'
fi
echo ""

# Quick links
echo "🔗 Quick Links:"
echo "   Admin Console:   http://localhost:${KEYCLOAK_PORT}"
echo "   Health Check:    http://localhost:${KEYCLOAK_MGMT_PORT}/health"
echo "   Metrics:         http://localhost:${KEYCLOAK_MGMT_PORT}/metrics"
echo ""

# Useful commands
echo "💡 Useful Commands:"
echo "   View logs:        ./scripts/logs.sh"
echo "   Restart:          ./scripts/restart-keycloak.sh"
echo "   Stop:             ./scripts/stop-keycloak.sh"
echo "   Build & Deploy:   ./scripts/build-deploy.sh"
