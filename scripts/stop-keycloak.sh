#!/bin/bash
set -e

echo "🛑 Stopping Keycloak..."
podman-compose down

echo ""
echo "✅ Keycloak stopped successfully!"
