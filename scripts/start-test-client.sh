#!/bin/bash
set -e

echo "🌐 Starting Web Test Client..."
echo ""

cd "$(dirname "$0")/../test-client"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo ""
fi

# Set environment variables
export KEYCLOAK_URL=http://localhost:8081
export KEYCLOAK_REALM=test-realm
export KEYCLOAK_CLIENT_ID=web-test-client
export KEYCLOAK_CLIENT_SECRET=web-test-client-secret-change-in-production
export REDIRECT_URI=http://localhost:3002/
export PORT=3002

# Start the server
npm start
