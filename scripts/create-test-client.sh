#!/bin/bash
set -e

REALM="${1:-test-realm}"
CLIENT="${2:-test-client}"

echo "👤 Creating test client in realm: $REALM"
echo "   Client ID: $CLIENT"
echo ""

podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh create clients -r $REALM \
  -s clientId=$CLIENT \
  -s enabled=true \
  -s publicClient=true \
  -s 'redirectUris=["http://localhost:3000/*"]' \
  -s 'webOrigins=["http://localhost:3000"]' \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=true

echo ""
echo "👥 Creating test user..."
podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh create users -r $REALM \
  -s username=testuser \
  -s enabled=true \
  -s email=testuser@example.com \
  -s firstName=Test \
  -s lastName=User

echo ""
echo "🔑 Setting password for test user..."
podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh set-password -r $REALM \
  --username testuser \
  --new-password test123

echo ""
echo "✅ Test client and user created successfully!"
echo ""
echo "📋 Details:"
echo "   Realm:         $REALM"
echo "   Client ID:     $CLIENT"
echo "   Username:      testuser"
echo "   Password:      test123"
echo ""
echo "🧪 Test Authorization Code Flow:"
echo "   open \"http://localhost:8080/realms/$REALM/protocol/openid-connect/auth?client_id=$CLIENT&redirect_uri=http://localhost:3000/callback&response_type=code&scope=openid\""
echo ""
echo "🧪 Test Direct Grant (Password Flow):"
echo "   http --form POST http://localhost:8080/realms/$REALM/protocol/openid-connect/token \\"
echo "     grant_type=password \\"
echo "     client_id=$CLIENT \\"
echo "     username=testuser \\"
echo "     password=test123 \\"
echo "     scope=openid"
