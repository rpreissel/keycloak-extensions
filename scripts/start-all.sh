#!/bin/bash

# Terminal tab 1: Test-Client starten
echo "🌐 Starting Web Test Client..."
cd /Users/rene/Develop/opencode/keycloak/test-client

export KEYCLOAK_URL=http://localhost:8081
export KEYCLOAK_REALM=test-realm
export KEYCLOAK_CLIENT_ID=web-test-client
export KEYCLOAK_CLIENT_SECRET=web-test-client-secret-change-in-production
export REDIRECT_URI=http://localhost:3002/
export PORT=3002

npm start &
TEST_CLIENT_PID=$!

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✅ Alle Systeme gestartet!"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "🔐 Keycloak Admin Console:"
echo "   URL:      http://localhost:8081"
echo "   Login:    admin / admin"
echo ""
echo "🌐 Web Test Client:"
echo "   URL:      http://localhost:3002"
echo "   Login:    testuser / test123"
echo ""
echo "📝 Test-Client Features:"
echo "   ✓ OAuth 2.0 Login"
echo "   ✓ Access Token Anzeige (JSON)"
echo "   ✓ ID Token Anzeige (JSON)"
echo "   ✓ Token Refresh"
echo "   ✓ Logout"
echo ""
echo "🛑 Zum Stoppen: Ctrl+C"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Wait for interrupt
wait $TEST_CLIENT_PID
