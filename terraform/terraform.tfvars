# Keycloak Connection (NO SECRETS HERE!)
keycloak_url    = "http://localhost:8081"
admin_username  = "admin"
# admin_password wird aus .env geladen via TF_VAR_admin_password

# Realm
realm_name         = "test-realm"
realm_display_name = "Test Realm"

# Client
client_id   = "test-client"
client_name = "Test Client"
redirect_uris = [
  "http://localhost:3000/*",
  "http://localhost:8081/realms/test-realm/*",  # For Mock IDP callback & Account Console
  "https://heise.de/*",
  "https://www.heise.de/*"
]
web_origins = [
  "http://localhost:3000",
  "http://localhost:8081",
  "https://heise.de",
  "https://www.heise.de"
]

# Test User
test_username         = "testuser"
test_user_email       = "testuser@example.com"
test_user_first_name  = "Test"
test_user_last_name   = "User"
# test_user_password wird aus .env geladen via TF_VAR_test_user_password

# Mock IDP Configuration
mock_idp_url     = "http://localhost/mock-idp"
mock_idp_enabled = true
