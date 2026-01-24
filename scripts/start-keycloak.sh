#!/bin/bash
set -e

# Load configuration
source "$(dirname "$0")/config.sh"

echo "🚀 Starting Keycloak with Podman Compose..."

# Check if podman machine is running
if ! podman ps >/dev/null 2>&1; then
    echo "⚠️  Podman machine is not running"
    echo "🔧 Starting podman machine..."
    podman machine start podman-machine-default
    echo "✅ Podman machine started"
    echo ""
fi

# Start containers
podman-compose up -d

echo ""
echo "⏳ Waiting for Keycloak to be ready..."
if ! ./scripts/wait-for-keycloak.sh 120; then
    echo "❌ Keycloak startup failed or timed out"
    echo "📝 Check logs with: ./scripts/logs.sh"
    exit 1
fi

echo ""
echo "✅ Keycloak started successfully!"
echo ""
echo "📊 Admin Console: http://localhost:${KEYCLOAK_PORT}"
echo "👤 Username: ${KEYCLOAK_ADMIN_USER}"
echo "🔑 Password: ${KEYCLOAK_ADMIN_PASS}"
echo "🐛 Debug Port: ${KEYCLOAK_DEBUG_PORT}"
echo ""
echo "📝 View logs with: ./scripts/logs.sh"
echo "💡 Check status: ./scripts/status.sh"
