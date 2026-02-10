FROM nginx:1.25-alpine

# Copy nginx template (will be processed by nginx's envsubst)
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy entrypoint wrapper to set NGINX_RESOLVER
COPY entrypoint-wrapper.sh /entrypoint-wrapper.sh
RUN chmod +x /entrypoint-wrapper.sh

# Create log directory
RUN mkdir -p /var/log/nginx && \
    chown -R nginx:nginx /var/log/nginx

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

EXPOSE 80

# Use wrapper to set NGINX_RESOLVER, then call original entrypoint
ENTRYPOINT ["/entrypoint-wrapper.sh"]
CMD ["nginx", "-g", "daemon off;"]
