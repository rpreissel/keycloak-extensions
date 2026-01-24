#!/bin/bash
set -e

SCRIPT_DIR="$(dirname "$0")"

# Load configuration
source "$SCRIPT_DIR/config.sh"

echo "🚀 Setting up complete Keycloak development environment"
echo "========================================================"
echo ""

# Start Keycloak
echo "1️⃣  Starting Keycloak..."
"$SCRIPT_DIR/start-keycloak.sh"

# Wait for Keycloak
echo ""
echo "2️⃣  Waiting for Keycloak to be ready..."
"$SCRIPT_DIR/wait-for-keycloak.sh"

# Initialize Terraform (if not done yet)
if [ ! -d "$SCRIPT_DIR/../terraform/.terraform" ]; then
  echo ""
  echo "3️⃣  Initializing OpenTofu..."
  "$SCRIPT_DIR/tf-init.sh"
fi

# Apply Terraform configuration
echo ""
echo "4️⃣  Applying OpenTofu configuration..."
"$SCRIPT_DIR/tf-apply.sh"

echo ""
echo "========================================="
echo "✅ Development environment ready!"
echo "========================================="
echo ""
echo "🎯 Quick Start:"
echo "   Admin Console:  http://localhost:${KEYCLOAK_PORT}"
echo "   Username:       ${KEYCLOAK_ADMIN_USER} / ${KEYCLOAK_ADMIN_PASS}"
echo "   Debug Port:     ${KEYCLOAK_DEBUG_PORT}"
echo ""
echo "🧪 Test User:"
echo "   Username:       testuser"
echo "   Password:       test123"
echo ""
echo "📝 Next steps:"
echo "   - Build extension:  ./scripts/build-deploy.sh"
echo "   - View logs:        ./scripts/logs.sh"
echo "   - View config:      cd terraform && tofu show"
echo "   - Modify config:    Edit terraform/*.tf or terraform/terraform.tfvars"
echo "   - Preview changes:  ./scripts/tf-plan.sh"
echo "   - Apply changes:    ./scripts/tf-apply.sh"
echo "   - Attach debugger:  F5 in VS Code"
echo "   - Check status:     ./scripts/status.sh"
