#!/bin/bash
set -e

SCRIPT_DIR="$(dirname "$0")"

# Load secrets from .env
if [ -f "$SCRIPT_DIR/.env" ]; then
  source "$SCRIPT_DIR/.env"
else
  echo "❌ Fehler: .env-Datei nicht gefunden!"
  echo "   Erstelle: $SCRIPT_DIR/.env"
  exit 1
fi

cd "$SCRIPT_DIR/../terraform"

echo "🔍 OpenTofu Plan - Preview der Änderungen"
echo "=========================================="
echo ""

tofu plan

echo ""
echo "📝 Um die Änderungen anzuwenden: ./scripts/tf-apply.sh"
