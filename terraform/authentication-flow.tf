# Authentication Flow Configuration
#
# With Identity Broker (OIDC IDP), we use Keycloak's standard browser flow.
# The standard flow automatically shows:
# 1. Username/Password form
# 2. All configured Identity Providers as buttons
#
# Users can:
# - Login with username/password (Keycloak local user)
# - Click "Mock Identity Provider" button (triggers Identity Broker flow)
# - Use kc_idp_hint=mock-idp to skip directly to Mock IDP
#
# No custom authentication flow needed - everything is handled by Keycloak's
# standard browser flow + Identity Provider configuration.
