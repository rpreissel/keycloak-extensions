#!/bin/bash
set -e

echo "🔄 Restarting Keycloak..."
podman restart keycloak-dev

echo ""
echo "⏳ Waiting for Keycloak to be ready..."
sleep 5

echo ""
echo "✅ Keycloak restarted successfully!"
echo "📝 View logs with: ./scripts/logs.sh"
