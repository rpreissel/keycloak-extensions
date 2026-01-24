#!/bin/bash
set -e

echo "🚀 Setting up complete Keycloak development environment"
echo "========================================================"
echo ""

# Check if realm name is provided
REALM="${1:-test-realm}"
CLIENT="${2:-test-client}"

echo "📋 Configuration:"
echo "   Realm:     $REALM"
echo "   Client:    $CLIENT"
echo ""

# Start Keycloak
echo "1️⃣  Starting Keycloak..."
./scripts/start-keycloak.sh

# Wait for Keycloak to be fully ready
echo ""
echo "⏳ Waiting for Keycloak to initialize (30 seconds)..."
sleep 30

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
echo "   Admin Console:  http://localhost:8080"
echo "   Username:       admin / admin"
echo "   Debug Port:     5005"
echo ""
echo "🧪 Test User:"
echo "   Username:       testuser"
echo "   Password:       test123"
echo ""
echo "📝 Next steps:"
echo "   - Build extension:  ./scripts/build-deploy.sh"
echo "   - View logs:        ./scripts/logs.sh"
echo "   - Attach debugger:  F5 in VS Code"
