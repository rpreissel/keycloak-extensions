#!/bin/bash
set -e

SCRIPT_DIR="$(dirname "$0")"

# Load secrets from .env
if [ -f "$SCRIPT_DIR/.env" ]; then
  source "$SCRIPT_DIR/.env"
else
  echo "❌ Fehler: .env-Datei nicht gefunden!"
  exit 1
fi

cd "$SCRIPT_DIR/../terraform"

echo "🚀 OpenTofu Apply - Änderungen anwenden"
echo "========================================"
echo ""

tofu apply -auto-approve

echo ""
echo "✅ Konfiguration angewendet!"
echo ""
echo "📋 Outputs:"
tofu output
