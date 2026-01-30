#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "🧪 Running all tests..."
    ./mvnw test
elif [[ "$1" == *"#"* ]]; then
    echo "🧪 Running single test method: $1"
    ./mvnw test -Dtest="$1"
else
    echo "🧪 Running test class: $1"
    ./mvnw test -Dtest="$1"
fi

echo ""
echo "✅ Tests completed!"
