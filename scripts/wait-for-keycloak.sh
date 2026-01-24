#!/bin/bash

# Wait for Keycloak to be fully ready
# Usage: ./wait-for-keycloak.sh [timeout_in_seconds]

# Load configuration
source "$(dirname "$0")/config.sh"

TIMEOUT="${1:-120}"
INTERVAL=5
ELAPSED=0

echo "⏳ Waiting for Keycloak to be ready (timeout: ${TIMEOUT}s)..."

while [ $ELAPSED -lt $TIMEOUT ]; do
    # Keycloak 26.x uses management interface on port 9000 for health checks
    if curl -sf http://localhost:9000/health/ready > /dev/null 2>&1; then
        echo "✅ Keycloak is ready!"
        exit 0
    fi
    
    echo "   Still waiting... (${ELAPSED}s elapsed)"
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

echo "❌ Timeout: Keycloak did not become ready in ${TIMEOUT} seconds"
echo "💡 Check logs with: ./scripts/logs.sh"
exit 1
