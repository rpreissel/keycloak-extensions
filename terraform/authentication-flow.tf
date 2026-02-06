# Mock IDP Authentication Flow Configuration
#
# This file configures the custom authentication flow that uses the Mock IDP authenticator.
# It creates a browser flow that redirects users to the Mock IDP for identity verification.

# Custom Authentication Flow for Mock IDP
resource "keycloak_authentication_flow" "mock_idp_browser_flow" {
  realm_id    = keycloak_realm.test_realm.id
  alias       = "mock-idp-browser"
  description = "Browser flow with Mock Identity Provider verification"
  provider_id = "basic-flow"
}

# Cookie Execution (check for existing session)
resource "keycloak_authentication_execution" "mock_idp_cookie" {
  realm_id          = keycloak_realm.test_realm.id
  parent_flow_alias = keycloak_authentication_flow.mock_idp_browser_flow.alias
  authenticator     = "auth-cookie"
  requirement       = "ALTERNATIVE"
}

# Mock IDP Authenticator - Custom SPI (direct login, no username/password)
resource "keycloak_authentication_execution" "mock_idp_authenticator" {
  realm_id          = keycloak_realm.test_realm.id
  parent_flow_alias = keycloak_authentication_flow.mock_idp_browser_flow.alias
  authenticator     = "mock-idp-authenticator"
  requirement       = var.mock_idp_enabled ? "ALTERNATIVE" : "DISABLED"
  depends_on        = [keycloak_authentication_execution.mock_idp_cookie]
}

# Bind the flow to the realm's browser flow
resource "keycloak_authentication_bindings" "mock_idp_bindings" {
  count        = var.mock_idp_enabled ? 1 : 0
  realm_id     = keycloak_realm.test_realm.id
  browser_flow = keycloak_authentication_flow.mock_idp_browser_flow.alias
}

# Authenticator Configuration for Mock IDP
resource "keycloak_authentication_execution_config" "mock_idp_config" {
  count             = var.mock_idp_enabled ? 1 : 0
  realm_id          = keycloak_realm.test_realm.id
  execution_id      = keycloak_authentication_execution.mock_idp_authenticator.id
  alias             = "mock-idp-config"
  
  config = {
    "mock.idp.url" = var.mock_idp_url
  }
}
