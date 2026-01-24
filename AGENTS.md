# Keycloak Extension Development - Agent Guide

## Project Overview

This is a Keycloak Extension (SPI) development project using Maven, Java 17+, and Podman Compose for containerization.

**Tech Stack:**
- Java 17+ (LTS)
- Maven 3.8+
- Keycloak 26.5.2 (Latest, Quarkus-based)
- Podman Compose (container orchestration)
- PostgreSQL 15 (database)

**Important**: This project is optimized for developer convenience. All workflows use scripts in `./scripts/` directory.

---

## Quick Start (Use Scripts!)

### Complete Setup (One Command)
```bash
./scripts/setup-dev-env.sh
```
This script:
- Starts Keycloak + PostgreSQL
- Creates test realm
- Creates test client and user
- Everything ready for development!

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

**Realm Setup:**
```bash
./scripts/setup-realm.sh                    # Create "test-realm"
./scripts/setup-realm.sh my-realm           # Create custom realm
./scripts/create-test-client.sh test-realm  # Create client + test user
```

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
| `setup-realm.sh [name]` | Create new realm |
| `create-test-client.sh [realm] [client]` | Create client + test user |
| `status.sh` | Show complete environment status |
| `wait-for-keycloak.sh [timeout]` | Wait for Keycloak health check |

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
