#!/bin/bash
set -e

# Load configuration
source "$(dirname "$0")/config.sh"

echo "👤 Creating Keycloak admin user manually..."
echo ""

# Create admin user using the Keycloak container
podman exec keycloak-dev bash -c "
    export KEYCLOAK_ADMIN=admin
    export KEYCLOAK_ADMIN_PASSWORD=admin
    cd /opt/keycloak
    ./bin/kc.sh start-dev --http-enabled=true --hostname-strict=false &
    sleep 5
    kill %1
"

echo "✅ Admin user should now be created"
echo ""
echo "Test with:"
echo "  curl -X POST http://localhost:${KEYCLOAK_PORT}/realms/master/protocol/openid-connect/token \\"
echo "    -d 'client_id=admin-cli' \\"
echo "    -d 'username=admin' \\"
echo "    -d 'password=admin' \\"
echo "    -d 'grant_type=password'"
