#!/bin/bash
set -e

# Load configuration
source "$(dirname "$0")/config.sh"

REALM="${1:-${DEFAULT_REALM}}"

echo "🔧 Setting up Keycloak Admin CLI..."

echo "🔐 Logging in as admin..."
podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user ${KEYCLOAK_ADMIN_USER} \
  --password ${KEYCLOAK_ADMIN_PASS}

echo ""
echo "🌍 Creating realm: $REALM"
podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh create realms \
  -s realm=$REALM \
  -s enabled=true \
  -s displayName="Test Realm"

echo ""
echo "✅ Realm '$REALM' created successfully!"
echo ""
echo "📝 Next steps:"
echo "   - Create clients: ./scripts/create-test-client.sh $REALM"
echo "   - View in Admin Console: http://localhost:${KEYCLOAK_PORT}/admin/master/console/#/$REALM"
