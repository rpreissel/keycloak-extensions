#!/bin/bash
set -e

# Load configuration
source "$(dirname "$0")/config.sh"

echo "🚀 Setting up complete Keycloak development environment"
echo "========================================================"
echo ""

# Check if realm name is provided
REALM="${1:-${DEFAULT_REALM}}"
CLIENT="${2:-${DEFAULT_CLIENT}}"

echo "📋 Configuration:"
echo "   Realm:     $REALM"
echo "   Client:    $CLIENT"
echo ""

# Start Keycloak
echo "1️⃣  Starting Keycloak..."
./scripts/start-keycloak.sh

# Setup realm
echo ""
echo "2️⃣  Creating realm..."
./scripts/setup-realm.sh "$REALM"

# Create test client and user
echo ""
echo "3️⃣  Creating test client and user..."
./scripts/create-test-client.sh "$REALM" "$CLIENT"

echo ""
echo "========================================="
echo "✅ Development environment ready!"
echo "========================================="
echo ""
echo "🎯 Quick Start:"
echo "   Admin Console:  http://localhost:${KEYCLOAK_PORT}"
echo "   Username:       ${KEYCLOAK_ADMIN_USER} / ${KEYCLOAK_ADMIN_PASS}"
echo "   Debug Port:     ${KEYCLOAK_DEBUG_PORT}"
echo ""
echo "🧪 Test User:"
echo "   Username:       testuser"
echo "   Password:       test123"
echo ""
echo "📝 Next steps:"
echo "   - Build extension:  ./scripts/build-deploy.sh"
echo "   - View logs:        ./scripts/logs.sh"
echo "   - Attach debugger:  F5 in VS Code"
echo "   - Check status:     ./scripts/status.sh"
