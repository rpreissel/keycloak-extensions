#!/bin/bash
set -e

echo "🔨 Building Keycloak Extensions..."
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "📦 Build successful!"
    echo "🔄 Triggering Keycloak rebuild..."
    
    podman exec keycloak-dev /opt/keycloak/bin/kc.sh build
    
    echo ""
    echo "✅ Deployment complete!"
    echo "📝 Check logs: podman logs -f keycloak-dev"
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi
