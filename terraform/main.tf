terraform {
  required_version = ">= 1.0"
  
  required_providers {
    keycloak = {
      source  = "keycloak/keycloak"
      version = "~> 5.6.0"
    }
  }
}

provider "keycloak" {
  client_id = "admin-cli"
  username  = var.admin_username
  password  = var.admin_password
  url       = var.keycloak_url
  realm     = "master"
}

# Realm
resource "keycloak_realm" "test_realm" {
  realm        = var.realm_name
  enabled      = true
  display_name = var.realm_display_name
  
  login_with_email_allowed = true
  
  # Token-Lifetimes
  access_token_lifespan = "5m"
}

# Client
resource "keycloak_openid_client" "test_client" {
  realm_id  = keycloak_realm.test_realm.id
  client_id = var.client_id
  
  name    = var.client_name
  enabled = true
  
  access_type = "PUBLIC"
  standard_flow_enabled = true
  direct_access_grants_enabled = true
  
  valid_redirect_uris = var.redirect_uris
  web_origins        = var.web_origins
}

# Web Test Client (CONFIDENTIAL for OAuth code flow)
resource "keycloak_openid_client" "web_test_client" {
  realm_id  = keycloak_realm.test_realm.id
  client_id = "web-test-client"
  
  name    = "Web Test Client"
  enabled = true
  
  access_type = "CONFIDENTIAL"
  standard_flow_enabled = true
  direct_access_grants_enabled = false
  
  valid_redirect_uris = [
    "http://localhost/app/*"
  ]
  web_origins = [
    "http://localhost"
  ]
  
  # Generate client secret
  client_secret = "web-test-client-secret-change-in-production"
}

# User
resource "keycloak_user" "test_user" {
  realm_id = keycloak_realm.test_realm.id
  username = var.test_username
  enabled  = true
  
  email      = var.test_user_email
  first_name = var.test_user_first_name
  last_name  = var.test_user_last_name
  
  initial_password {
    value     = var.test_user_password
    temporary = false
  }
}

# Identity Provider - Mock IDP (OIDC)
resource "keycloak_oidc_identity_provider" "mock_idp" {
  realm             = keycloak_realm.test_realm.id
  alias             = "mock-idp"
  display_name      = "Mock Identity Provider"
  enabled           = true
  
  # Trust email from IDP
  trust_email       = true
  
  # Sync mode - import user if not exists
  sync_mode         = "IMPORT"
  
  # OIDC Configuration
  authorization_url = "http://localhost/mock-idp/authorize"
  token_url         = "http://reverse-proxy/mock-idp/token"  # Internal container URL
  client_id         = "keycloak"
  client_secret     = "not-used-in-public-flow"
  
  # Default scopes
  default_scopes    = "openid profile email"
  
  # UI settings
  hide_on_login_page = false
  
  # Additional configuration
  extra_config = {
    "clientAuthMethod" = "client_secret_post"
  }
}

# Attribute Mappers for Mock IDP
resource "keycloak_custom_identity_provider_mapper" "mock_idp_username" {
  realm                    = keycloak_realm.test_realm.id
  name                     = "username"
  identity_provider_alias  = keycloak_oidc_identity_provider.mock_idp.alias
  identity_provider_mapper = "oidc-user-attribute-idp-mapper"

  extra_config = {
    claim              = "sub"
    "user.attribute"   = "username"
    syncMode           = "INHERIT"
  }
}

resource "keycloak_custom_identity_provider_mapper" "mock_idp_email" {
  realm                    = keycloak_realm.test_realm.id
  name                     = "email"
  identity_provider_alias  = keycloak_oidc_identity_provider.mock_idp.alias
  identity_provider_mapper = "oidc-user-attribute-idp-mapper"

  extra_config = {
    claim              = "email"
    "user.attribute"   = "email"
    syncMode           = "INHERIT"
  }
}

resource "keycloak_custom_identity_provider_mapper" "mock_idp_first_name" {
  realm                    = keycloak_realm.test_realm.id
  name                     = "firstName"
  identity_provider_alias  = keycloak_oidc_identity_provider.mock_idp.alias
  identity_provider_mapper = "oidc-user-attribute-idp-mapper"

  extra_config = {
    claim              = "given_name"
    "user.attribute"   = "firstName"
    syncMode           = "INHERIT"
  }
}

resource "keycloak_custom_identity_provider_mapper" "mock_idp_last_name" {
  realm                    = keycloak_realm.test_realm.id
  name                     = "lastName"
  identity_provider_alias  = keycloak_oidc_identity_provider.mock_idp.alias
  identity_provider_mapper = "oidc-user-attribute-idp-mapper"

  extra_config = {
    claim              = "family_name"
    "user.attribute"   = "lastName"
    syncMode           = "INHERIT"
  }
}

resource "keycloak_custom_identity_provider_mapper" "mock_idp_verification_status" {
  realm                    = keycloak_realm.test_realm.id
  name                     = "verificationStatus"
  identity_provider_alias  = keycloak_oidc_identity_provider.mock_idp.alias
  identity_provider_mapper = "oidc-user-attribute-idp-mapper"

  extra_config = {
    claim              = "verification_status"
    "user.attribute"   = "verificationStatus"
    syncMode           = "INHERIT"
  }
}
