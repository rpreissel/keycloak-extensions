# Keycloak Extension Development - Agent Guide

## Project Overview

This is a Keycloak Extension (SPI) development project using Maven, Java 17+, and Podman Compose for containerization.

**Tech Stack:**
- Java 17+ (LTS)
- Maven 3.8+
- Keycloak 26.5.2 (Latest, Quarkus-based)
- Podman Compose (container orchestration)
- PostgreSQL 15 (database)
- **OpenTofu** (open-source declarative Keycloak configuration)

**Important**: This project uses **declarative configuration** via OpenTofu. All workflows use scripts in `./scripts/` directory.

---

## Quick Start (Use Scripts!)

### Complete Setup (One Command)
```bash
./scripts/setup-dev-env.sh
```
This script:
- Starts Keycloak + PostgreSQL
- Initializes OpenTofu (if needed)
- Applies declarative configuration (realm, client, user)
- Everything ready for development!

**Key Difference**: Configuration is now **declarative** (defined in `terraform/*.tf` files), not imperative scripts.

### Individual Scripts

**Container Management:**
```bash
./scripts/start-keycloak.sh     # Start Keycloak with Podman Compose
./scripts/stop-keycloak.sh      # Stop Keycloak
./scripts/restart-keycloak.sh   # Restart Keycloak
./scripts/logs.sh               # View logs (follow mode)
```

**Build & Deploy:**
```bash
./scripts/build-deploy.sh       # Build Maven project + deploy to container
```

**Testing:**
```bash
./scripts/test.sh                      # Run all tests
./scripts/test.sh MyTestClass          # Run single test class
./scripts/test.sh MyTestClass#method   # Run single test method
```

**Debugging:**
```bash
./scripts/debug.sh              # Show debug info and test connection
```

**OpenTofu Configuration (Declarative):**
```bash
./scripts/tf-init.sh                # Initialize OpenTofu (one-time)
./scripts/tf-plan.sh                # Preview configuration changes
./scripts/tf-apply.sh               # Apply configuration
./scripts/tf-destroy.sh             # Remove all OpenTofu-managed resources
```

**Legacy Realm Setup (Removed):**
Previously used imperative scripts (`setup-realm.sh`, `create-test-client.sh`) - now replaced by declarative OpenTofu configuration.

---

## Build Commands (Manual)

**Only use these if scripts don't work!**

### Build All Modules
```bash
mvn clean package
```

### Build & Deploy
```bash
mvn clean package
podman exec keycloak-dev /opt/keycloak/bin/kc.sh build
```

---

## Test Commands (Manual)

**Prefer using `./scripts/test.sh` instead!**

```bash
mvn test                              # All tests
mvn test -Dtest=MyTestClass           # Single class
mvn test -Dtest=MyTestClass#method    # Single method
mvn clean package -DskipTests         # Skip tests
```

---

## Code Style Guidelines

### Package Structure
```
src/main/java/com/example/keycloak/
├── authenticator/        # Custom authenticators
├── provider/             # Provider implementations
├── mapper/               # Protocol mappers
└── listener/             # Event listeners

src/main/resources/
└── META-INF/
    ├── services/         # SPI registration files
    └── beans.xml         # CDI activation
```

### Imports
- **Order**: Java standard → Jakarta EE → Keycloak → Third-party → Internal
- **No wildcards**: Always use explicit imports
- **Organize**: Alphabetical within groups

### Naming Conventions
- **Classes**: `PascalCase` (e.g., `CustomAuthenticator`, `MyProviderFactory`)
- **Interfaces**: `PascalCase` (e.g., `TokenValidator`)
- **Methods**: `camelCase` (e.g., `authenticate()`, `validateToken()`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `PROVIDER_ID`, `MAX_RETRY_COUNT`)
- **Variables**: `camelCase` (e.g., `authSession`, `userModel`)
- **Packages**: `lowercase` (e.g., `com.example.keycloak.authenticator`)

### SPI Provider Pattern
Every extension follows this pattern:

1. **Provider**: Implements business logic
   ```java
   public class MyAuthenticator implements Authenticator {
       // Business logic here
   }
   ```

2. **ProviderFactory**: Creates provider instances
   ```java
   public class MyAuthenticatorFactory implements AuthenticatorFactory {
       public static final String PROVIDER_ID = "my-authenticator";
       // Factory methods here
   }
   ```

3. **Service Registration**: Create file `META-INF/services/org.keycloak.authentication.AuthenticatorFactory`
   ```
   com.example.keycloak.authenticator.MyAuthenticatorFactory
   ```

### Dependencies - CRITICAL!
**ALL Keycloak dependencies MUST use `provided` scope:**

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-core</artifactId>
    <scope>provided</scope>
</dependency>
```

**Why?** Keycloak libraries are already in the container. Bundling them causes conflicts.

### Logging
Use JBoss Logging (provided by Keycloak):

```java
import org.jboss.logging.Logger;

public class MyAuthenticator {
    private static final Logger logger = Logger.getLogger(MyAuthenticator.class);
    
    public void authenticate() {
        logger.info("Authenticating user");
        logger.debugf("Session ID: %s", sessionId);
        logger.errorf(e, "Authentication failed for user: %s", username);
    }
}
```

### Error Handling
```java
try {
    // authentication logic
} catch (AuthenticationException e) {
    logger.errorf(e, "Authentication failed for user: %s", username);
    context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
}
```

**Rules:**
- Don't catch generic `Exception` - be specific
- Always log errors with context
- Use Keycloak exceptions: `AuthenticationFlowException`, `ErrorResponseException`
- Fail gracefully with meaningful error responses

### Types & Nullability
- **Prefer interfaces**: Use `UserModel` over concrete implementations
- **Null checks**: Always check Keycloak models for null before use
- **Optional**: Use `Optional<T>` for potentially missing values
- **Immutability**: Make fields `final` where possible

### Code Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Braces**: Always use braces for if/for/while (even single line)
- **Method length**: Keep methods under 50 lines
- **Comments**: JavaDoc for public APIs, inline for complex logic

---

## Container Management (Podman Compose)

**Always use scripts!**

```bash
./scripts/start-keycloak.sh      # Start with podman-compose
./scripts/stop-keycloak.sh       # Stop containers
./scripts/restart-keycloak.sh    # Restart Keycloak
./scripts/logs.sh                # View logs
```

**Manual commands (if needed):**
```bash
podman-compose up -d             # Start
podman-compose down              # Stop
podman logs -f keycloak-dev      # Logs
podman ps -a                     # Container status
podman exec -it keycloak-dev /bin/bash  # Shell into container
```

---

## Debugging

**Remote Debug Port**: `localhost:5005`

### Quick Check
```bash
./scripts/debug.sh
```

### VS Code
Press `F5` or use Run → Start Debugging

Configuration in `.vscode/launch.json`:
```json
{
  "type": "java",
  "name": "Debug Keycloak",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

### IntelliJ IDEA
Run → Edit Configurations → Remote JVM Debug
- Host: `localhost`
- Port: `5005`

---

## Keycloak Configuration with OpenTofu

**Philosophy**: Use **declarative** configuration instead of imperative scripts. Define the desired state, OpenTofu makes it happen.

**What is OpenTofu?** Open-source Terraform fork (MPL 2.0 license), fully compatible with Terraform configurations.

### Configuration Structure

```
terraform/
├── main.tf              # Provider config + resources (realm, client, user)
├── variables.tf         # Input variables
├── outputs.tf           # Outputs (realm ID, URLs, etc.)
├── terraform.tfvars     # Default values (NO SECRETS!)
└── .gitignore           # OpenTofu cache
```

### Configuration Files

**Main Resources** (`terraform/main.tf`):
- Keycloak realm (test-realm)
- OpenID Connect client (test-client)
- Test user with password

**Variables** (`terraform/variables.tf`):
- Keycloak connection (URL, admin credentials)
- Realm settings
- Client settings
- User settings

**Secrets** (`scripts/.env`):
- Admin password: `TF_VAR_admin_password`
- Test user password: `TF_VAR_test_user_password`

### Workflow: Changing Configuration

1. **Edit configuration**:
   ```bash
   vim terraform/terraform.tfvars  # Change values
   # OR
   vim terraform/main.tf           # Change resources
   ```

2. **Preview changes**:
   ```bash
   ./scripts/tf-plan.sh
   ```
   This shows **exactly** what will change (add/modify/delete).

3. **Apply changes**:
   ```bash
   ./scripts/tf-apply.sh
   ```
   Terraform updates Keycloak to match your configuration.

### Example: Add a New Client

Edit `terraform/main.tf`, add:

```hcl
resource "keycloak_openid_client" "my_new_client" {
  realm_id  = keycloak_realm.test_realm.id
  client_id = "my-new-client"
  name      = "My New Client"
  enabled   = true
  
  access_type = "CONFIDENTIAL"
  valid_redirect_uris = ["https://app.example.com/*"]
  
  # Get client secret
  client_secret = "supersecret"  # Or use random provider
}
```

Then:
```bash
./scripts/tf-plan.sh   # Preview
./scripts/tf-apply.sh  # Apply
```

### Drift Detection

Check if someone manually changed Keycloak configuration:

```bash
./scripts/tf-plan.sh
# -> Zeigt ob jemand manuell was geändert hat
```

**Fix drift**:
```bash
./scripts/tf-apply.sh  # Restore to declared state
```

### State Management

OpenTofu tracks what it created in **state files** (`terraform.tfstate`).

**Important**:
- State files are in `.gitignore` (contain secrets!)
- For teams: Use remote state (S3, OpenTofu Cloud)
- Local development: State stays local

### Importing Existing Configuration

If you already have a realm/client in Keycloak:

```bash
cd terraform
tofu import keycloak_realm.test_realm test-realm
tofu import keycloak_openid_client.test_client test-realm/client-uuid
```

Get UUIDs from Admin Console or API.

---

## Development Workflow

### For Agent: Complete Development Cycle

1. **Initial Setup** (one time):
   ```bash
   ./scripts/setup-dev-env.sh
   ```

2. **Write Code**:
   - Implement Provider class
   - Implement ProviderFactory class
   - Create SPI registration file in `META-INF/services/`

3. **Build & Deploy**:
   ```bash
   ./scripts/build-deploy.sh
   ```

4. **Test**:
   - Trigger extension via Admin Console or API
   - Check logs: `./scripts/logs.sh`

5. **Debug** (if needed):
   - Set breakpoints in IDE
   - Attach debugger: `F5` in VS Code
   - Trigger extension code

6. **Run Tests**:
   ```bash
   ./scripts/test.sh
   ```

7. **Iterate**: Repeat steps 2-6

### Important Workflow Notes
- Always use `./scripts/build-deploy.sh` after code changes
- Container automatically picks up new JARs from `providers/` directory
- No manual copying needed - volume mount handles it
- Check logs immediately after deploy to verify extension loaded

---

## Scripts Reference

All scripts are in `./scripts/` and are executable.

| Script | Purpose |
|--------|---------|
| `setup-dev-env.sh` | Complete setup: Start containers, create realm, client, user |
| `start-keycloak.sh` | Start Keycloak + PostgreSQL with podman-compose |
| `stop-keycloak.sh` | Stop all containers |
| `restart-keycloak.sh` | Restart Keycloak container |
| `build-deploy.sh` | Maven build + deploy to container |
| `test.sh [class]` | Run tests (all, class, or method) |
| `logs.sh` | View Keycloak logs (follow mode) |
| `debug.sh` | Show debug info and test connection |
| `status.sh` | Show complete environment status |
| `wait-for-keycloak.sh [timeout]` | Wait for Keycloak health check |
| `tf-init.sh` | Initialize OpenTofu (one-time) |
| `tf-plan.sh` | Preview OpenTofu configuration changes |
| `tf-apply.sh` | Apply OpenTofu configuration |
| `tf-destroy.sh` | Destroy all OpenTofu-managed resources |

---

## Best Practices for Agents

1. **Always use scripts** - Don't run manual commands unless scripts fail
2. **Use `provided` scope** for ALL Keycloak dependencies
3. **Use fully qualified names** in SPI service registration files
4. **Log appropriately**: INFO for user actions, DEBUG for details
5. **Test locally** before considering complete
6. **Keep JARs small** - no unnecessary dependencies
7. **Follow SPI contracts** - implement all required methods
8. **Check for null** - Keycloak models can return null
9. **After code changes**: Always run `./scripts/build-deploy.sh`
10. **Verify deployment**: Check logs with `./scripts/logs.sh`

---

## Admin Console & URLs

- **Admin Console**: http://localhost:8081
- **Username**: admin
- **Password**: admin
- **Debug Port**: localhost:5005
- **Management Interface**: http://localhost:9000 (health, metrics)
- **PostgreSQL**: localhost:5432 (keycloak/keycloak)

### Health & Monitoring
- **Health Check**: http://localhost:9000/health
- **Metrics**: http://localhost:9000/metrics
- **Ready Check**: http://localhost:9000/health/ready

---

## Troubleshooting

### Extension not loading?
```bash
./scripts/logs.sh | grep -i error
podman exec keycloak-dev ls -la /opt/keycloak/providers/
```

### Debug connection fails?
```bash
./scripts/debug.sh
```

### Container won't start?
```bash
podman-compose down
podman-compose up -d
./scripts/logs.sh
```

---

## Resources

- **Keycloak Docs**: https://www.keycloak.org/docs/26.5/
- **Server Developer Guide**: https://www.keycloak.org/docs/26.5/server_development/
- **API JavaDocs**: https://www.keycloak.org/docs-api/26.5.2/javadocs/
