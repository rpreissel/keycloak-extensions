#!/bin/bash
set -e

echo "🔨 Building Keycloak Extensions..."

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found in current directory"
    echo "💡 Run this script from the project root"
    exit 1
fi

# Build with Maven
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "📦 Build successful!"
    
    # Check if JAR files were created
    JAR_COUNT=$(find . -name "*.jar" -not -path "*/target/*" -type f 2>/dev/null | wc -l)
    if [ $JAR_COUNT -eq 0 ]; then
        echo "⚠️  Warning: No JAR files found outside target directories"
        echo "💡 Extensions should be in providers/ directory"
    fi
    
    # Copy JARs to providers directory
    echo "📋 Copying JARs to providers directory..."
    find . -path "*/target/*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -exec cp {} providers/ \;
    
    echo "🔄 Triggering Keycloak rebuild..."
    
    # Check if Keycloak container is running
    if ! podman ps --filter "name=keycloak-dev" --format "{{.Names}}" | grep -q "keycloak-dev"; then
        echo "⚠️  Warning: Keycloak container is not running"
        echo "💡 Start it with: ./scripts/start-keycloak.sh"
        exit 1
    fi
    
    podman exec keycloak-dev /opt/keycloak/bin/kc.sh build
    
    echo ""
    echo "✅ Deployment complete!"
    echo "📝 Check logs: ./scripts/logs.sh"
    echo "💡 Restart Keycloak: ./scripts/restart-keycloak.sh"
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi
