#!/bin/bash
set -e

SCRIPT_DIR="$(dirname "$0")"
cd "$SCRIPT_DIR/../terraform"

echo "🔧 Initialisiere OpenTofu..."
echo ""

tofu init

echo ""
echo "✅ OpenTofu initialisiert!"
echo ""
echo "📝 Nächste Schritte:"
echo "   - Preview:  ./scripts/tf-plan.sh"
echo "   - Anwenden: ./scripts/tf-apply.sh"
