#!/bin/bash
set -e

REALM="${1:-test-realm}"

echo "🔧 Setting up Keycloak Admin CLI alias..."
alias kcadm='podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh'

echo "🔐 Logging in as admin..."
podman exec -it keycloak-dev /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

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
echo "   - View in Admin Console: http://localhost:8080/admin/master/console/#/$REALM"
