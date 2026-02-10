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
