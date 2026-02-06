#!/bin/bash
set -e

cd "$(dirname "$0")/.."

echo "========================================="
echo "Stopping Mock Identity Provider"
echo "========================================="

podman-compose stop mock-idp

echo "✓ Mock IDP stopped"
echo ""
