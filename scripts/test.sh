#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "🧪 Running all tests..."
    mvn test
elif [[ "$1" == *"#"* ]]; then
    echo "🧪 Running single test method: $1"
    mvn test -Dtest="$1"
else
    echo "🧪 Running test class: $1"
    mvn test -Dtest="$1"
fi

echo ""
echo "✅ Tests completed!"
