# Keycloak Connection
variable "keycloak_url" {
  description = "Keycloak Server URL"
  type        = string
}

variable "admin_username" {
  description = "Keycloak Admin Username"
  type        = string
}

variable "admin_password" {
  description = "Keycloak Admin Password (from .env)"
  type        = string
  sensitive   = true
}

# Realm
variable "realm_name" {
  description = "Realm name"
  type        = string
}

variable "realm_display_name" {
  description = "Realm display name"
  type        = string
}

# Client
variable "client_id" {
  description = "Client ID"
  type        = string
}

variable "client_name" {
  description = "Client display name"
  type        = string
}

variable "redirect_uris" {
  description = "Valid redirect URIs"
  type        = list(string)
}

variable "web_origins" {
  description = "Web origins (CORS)"
  type        = list(string)
}

# Test User
variable "test_username" {
  description = "Test user username"
  type        = string
}

variable "test_user_email" {
  description = "Test user email"
  type        = string
}

variable "test_user_first_name" {
  description = "Test user first name"
  type        = string
}

variable "test_user_last_name" {
  description = "Test user last name"
  type        = string
}

variable "test_user_password" {
  description = "Test user password (from .env)"
  type        = string
  sensitive   = true
}
