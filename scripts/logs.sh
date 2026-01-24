#!/bin/bash
set -e

echo "📋 Showing Keycloak logs..."
echo "   Press Ctrl+C to exit"
echo ""

podman logs -f keycloak-dev
