#!/bin/bash
set -e

echo "🐛 Debug Information"
echo "===================="
echo ""
echo "📌 Debug Port:     localhost:5005"
echo "📌 Protocol:       JDWP"
echo ""
echo "VS Code:"
echo "   Press F5 to attach debugger"
echo ""
echo "IntelliJ IDEA:"
echo "   Run → Debug 'Keycloak Debug'"
echo ""
echo "Testing Debug Connection:"
nc -zv localhost 5005 2>&1 | grep -q "succeeded" && echo "✅ Debug port is accessible" || echo "❌ Debug port is NOT accessible"
echo ""
echo "📝 Container logs:"
podman logs --tail 20 keycloak-dev
