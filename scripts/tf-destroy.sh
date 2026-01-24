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

echo "⚠️  OpenTofu Destroy - LÖSCHT ALLE Ressourcen!"
echo "==============================================="
echo ""
read -p "Bist du sicher? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
  echo "Abgebrochen."
  exit 0
fi

tofu destroy

echo ""
echo "✅ Alle OpenTofu-verwalteten Ressourcen gelöscht!"
