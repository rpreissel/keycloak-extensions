# Mock IDP Client Scopes Configuration
#
# This file defines custom client scopes for Mock IDP user attributes.
# These scopes allow clients to request Mock IDP specific claims in tokens.

# Client Scope for Mock IDP Attributes
resource "keycloak_openid_client_scope" "mock_idp_attributes" {
  realm_id               = keycloak_realm.test_realm.id
  name                   = "mock-idp-attributes"
  description            = "Mock IDP user attributes (verification status, birthdate, etc.)"
  include_in_token_scope = true
  gui_order              = 1
}

# Protocol Mapper: Verification Status
resource "keycloak_openid_user_attribute_protocol_mapper" "verification_status" {
  realm_id            = keycloak_realm.test_realm.id
  client_scope_id     = keycloak_openid_client_scope.mock_idp_attributes.id
  name                = "verification-status"
  user_attribute      = "verification_status"
  claim_name          = "verification_status"
  claim_value_type    = "String"
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

# Protocol Mapper: Birthdate
resource "keycloak_openid_user_attribute_protocol_mapper" "birthdate" {
  realm_id            = keycloak_realm.test_realm.id
  client_scope_id     = keycloak_openid_client_scope.mock_idp_attributes.id
  name                = "birthdate"
  user_attribute      = "birthdate"
  claim_name          = "birthdate"
  claim_value_type    = "String"
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

# Protocol Mapper: Mock IDP User ID
resource "keycloak_openid_user_attribute_protocol_mapper" "mock_idp_user_id" {
  realm_id            = keycloak_realm.test_realm.id
  client_scope_id     = keycloak_openid_client_scope.mock_idp_attributes.id
  name                = "mock-idp-user-id"
  user_attribute      = "mock_idp_user_id"
  claim_name          = "mock_idp_user_id"
  claim_value_type    = "String"
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

# Protocol Mapper: Full Name
resource "keycloak_openid_user_attribute_protocol_mapper" "full_name" {
  realm_id            = keycloak_realm.test_realm.id
  client_scope_id     = keycloak_openid_client_scope.mock_idp_attributes.id
  name                = "full-name"
  user_attribute      = "full_name"
  claim_name          = "full_name"
  claim_value_type    = "String"
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

# Add scope to test client as optional scope
resource "keycloak_openid_client_optional_scopes" "test_client_mock_idp_scope" {
  count        = var.mock_idp_enabled ? 1 : 0
  realm_id     = keycloak_realm.test_realm.id
  client_id    = keycloak_openid_client.test_client.id
  optional_scopes = [
    keycloak_openid_client_scope.mock_idp_attributes.name
  ]
}
