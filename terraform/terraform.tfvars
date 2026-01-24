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
  "http://localhost:3000/*"
]
web_origins = [
  "http://localhost:3000"
]

# Test User
test_username         = "testuser"
test_user_email       = "testuser@example.com"
test_user_first_name  = "Test"
test_user_last_name   = "User"
# test_user_password wird aus .env geladen via TF_VAR_test_user_password
