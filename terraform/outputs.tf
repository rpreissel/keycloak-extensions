output "realm_id" {
  description = "Realm ID"
  value       = keycloak_realm.test_realm.id
}

output "client_id" {
  description = "Client ID"
  value       = keycloak_openid_client.test_client.client_id
}

output "test_user_username" {
  description = "Test user username"
  value       = keycloak_user.test_user.username
}

output "web_test_client_id" {
  description = "Web Test Client ID"
  value       = keycloak_openid_client.web_test_client.client_id
}

output "web_test_client_secret" {
  description = "Web Test Client Secret"
  value       = keycloak_openid_client.web_test_client.client_secret
  sensitive   = true
}

output "auth_url" {
  description = "Authorization URL for testing"
  value       = "http://localhost:8081/realms/${var.realm_name}/protocol/openid-connect/auth?client_id=${var.client_id}&redirect_uri=http://localhost:3000/callback&response_type=code&scope=openid"
}

output "web_test_client_url" {
  description = "Web Test Client Application URL"
  value       = "http://localhost/app"
}
