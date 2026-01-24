#!/bin/bash
set -e

echo "🚀 Starting Keycloak with Podman Compose..."
podman-compose up -d

echo ""
echo "⏳ Waiting for Keycloak to be ready..."
sleep 10

echo ""
echo "✅ Keycloak started successfully!"
echo ""
echo "📊 Admin Console: http://localhost:8080"
echo "👤 Username: admin"
echo "🔑 Password: admin"
echo "🐛 Debug Port: 5005"
echo ""
echo "📝 View logs with: podman logs -f keycloak-dev"
