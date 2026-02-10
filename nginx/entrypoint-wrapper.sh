#!/bin/sh
# Simple wrapper to set NGINX_RESOLVER from /etc/resolv.conf

export NGINX_RESOLVER=$(awk '/^nameserver/ {print $2; exit}' /etc/resolv.conf)
echo "🔍 Using DNS resolver: $NGINX_RESOLVER"

# Execute original nginx entrypoint
exec /docker-entrypoint.sh "$@"
