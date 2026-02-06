#!/bin/bash
set -e

cd "$(dirname "$0")/.."

echo "========================================="
echo "Starting Mock Identity Provider"
echo "========================================="

# Build and start mock-idp service
echo "Building and starting Mock IDP container..."
podman-compose up -d --build mock-idp

echo ""
echo "Waiting for Mock IDP to be ready..."
sleep 3

# Check health
for i in {1..10}; do
    if curl -f -s http://localhost:3001/health > /dev/null 2>&1; then
        echo "✓ Mock IDP is healthy!"
        break
    fi
    echo "  Waiting... ($i/10)"
    sleep 2
done

echo ""
echo "========================================="
echo "Mock Identity Provider Started"
echo "========================================="
echo "  Service:    http://localhost:3001"
echo "  Health:     http://localhost:3001/health"
echo "  JWKS:       http://localhost:3001/.well-known/jwks.json"
echo "========================================="
echo ""
echo "Test URL example:"
echo "http://localhost:3001/verify?client_id=test-client&transaction_id=test-123&callback_token=token-456&state=abc&redirect_uri=http://localhost:8081/auth/callback"
echo ""
