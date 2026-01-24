# Keycloak Testcontainer Integration Test

This integration test demonstrates how to use Testcontainers with Keycloak, PostgreSQL, and Terraform to create fully automated, reproducible integration tests.

## Overview

The test suite (`KeycloakLoginIntegrationTest`) performs the following:

1. **Starts PostgreSQL container** with Testcontainers
2. **Starts Keycloak container** connected to PostgreSQL
3. **Applies Terraform configuration** to create realm, client, and user
4. **Tests login functionality** with password grant
5. **Cleans up** all resources after tests complete

## Test Cases

The integration test includes the following test cases:

### ✅ testKeycloakIsRunning
Verifies that Keycloak container is up and running.

### ✅ testPasswordGrantLogin
Tests successful login with valid credentials:
- Requests access token via password grant (OAuth 2.0 Direct Grant)
- Validates token response structure
- Verifies token type is "Bearer"
- Extracts and validates access token

### ✅ testInvalidPasswordLogin
Tests authentication failure with invalid credentials:
- Attempts login with wrong password
- Expects HTTP 401 Unauthorized
- Validates error response contains "invalid_grant"

### ✅ testRealmConfiguration
Verifies realm configuration:
- Fetches realm information from OpenID Connect discovery endpoint
- Validates realm name matches expected value

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Test Execution                        │
│                  (JUnit 5 + Maven)                       │
└──────────────────┬──────────────────────────────────────┘
                   │
         ┌─────────┴──────────┐
         │  Testcontainers    │
         │   (@Container)     │
         └─────────┬──────────┘
                   │
       ┌───────────┴───────────────┐
       │                           │
   ┌───▼────┐               ┌──────▼──────┐
   │ Postgres│               │  Keycloak   │
   │Container│◄──────────────│  Container  │
   └─────────┘   Network     └──────┬──────┘
                                    │
                             ┌──────▼──────┐
                             │  Terraform  │
                             │(OpenTofu CLI)│
                             └─────────────┘
```

## Dependencies

### Maven Dependencies (`pom.xml`)

```xml
<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- HTTP Client -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3</version>
    <scope>test</scope>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
    <scope>test</scope>
</dependency>
```

## Prerequisites

1. **Docker or Podman**: Testcontainers requires a container runtime
2. **OpenTofu**: For applying Terraform configuration
3. **Java 17+**: Required by Keycloak and the test
4. **Maven 3.8+**: For building and running tests

## Running the Tests

### Run all integration tests
```bash
mvn test
```

### Run only the Keycloak integration test
```bash
mvn test -Dtest=KeycloakLoginIntegrationTest
```

### Run with Maven wrapper
```bash
./mvnw test -Dtest=KeycloakLoginIntegrationTest
```

### Run with our script
```bash
./scripts/test.sh KeycloakLoginIntegrationTest
```

## How It Works

### 1. Container Setup

The test uses Testcontainers annotations to declare containers:

```java
@Testcontainers
public class KeycloakLoginIntegrationTest {
    
    @Container
    private static PostgreSQLContainer<?> postgresContainer = ...;
    
    @Container  
    private static GenericContainer<?> keycloakContainer = ...;
    
    @BeforeAll
    public static void setUp() throws Exception {
        // Apply Terraform configuration
        applyTerraformConfiguration();
    }
}
```

### 2. Terraform Integration

The test dynamically creates a Terraform variables file with the container's URL:

```java
private static void applyTerraformConfiguration() {
    // Create tfvars file with dynamic Keycloak URL
    String tfvarsContent = String.format(
        "keycloak_url = \"%s\"\n" +
        "realm_name = \"%s\"\n" +
        ...,
        keycloakUrl, REALM_NAME
    );
    
    // Run: tofu init && tofu apply -auto-approve
    runCommand(terraformDir, "tofu", "init");
    runCommand(terraformDir, "tofu", "apply", "-auto-approve");
}
```

### 3. Test Execution

Tests use Apache HttpClient to interact with Keycloak:

```java
@Test
public void testPasswordGrantLogin() throws Exception {
    HttpPost httpPost = new HttpPost(tokenUrl);
    
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("grant_type", "password"));
    params.add(new BasicNameValuePair("client_id", CLIENT_ID));
    params.add(new BasicNameValuePair("username", USERNAME));
    params.add(new BasicNameValuePair("password", PASSWORD));
    
    CloseableHttpResponse response = httpClient.execute(httpPost);
    
    assertEquals(200, response.getCode());
    // ... validate token response
}
```

### 4. Cleanup

Terraform state is destroyed after tests:

```java
@AfterAll
public static void tearDown() throws Exception {
    destroyTerraformConfiguration();
    // Containers auto-stopped by Testcontainers
}
```

## Test Output Example

```
Keycloak started at: http://localhost:33731
Applying Terraform configuration...
Running: tofu init
Running: tofu apply -auto-approve

keycloak_realm.test_realm: Creating...
keycloak_realm.test_realm: Creation complete

Token response status: 200
✅ Login successful!
Access Token (first 50 chars): eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6...

Invalid login status: 401
✅ Invalid password correctly rejected

Realm: test-realm
✅ Realm configuration verified

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

## Key Features

### Fully Isolated
- Each test run creates fresh containers
- No interference with other tests or manual testing
- Cleanup is automatic

### Declarative Configuration
- Uses Terraform/OpenTofu for realm setup
- Configuration as code
- Reproducible across environments

### Fast Feedback
- Parallel container startup
- Efficient wait strategies
- ~15-20 seconds total execution time

### Comprehensive Coverage
- Tests actual OAuth 2.0 flows
- Validates error handling
- Verifies configuration integrity

## Troubleshooting

### Containers not starting
```bash
# Check Docker/Podman status
docker ps -a
# or
podman ps -a

# Increase timeout in test
.waitingFor(Wait.forHttp("/")
    .withStartupTimeout(Duration.ofMinutes(10)))
```

### Terraform errors
```bash
# Clean Terraform state
cd terraform
rm -rf .terraform/ terraform.tfstate*
tofu init
```

### Port conflicts
Testcontainers automatically assigns random ports, so conflicts should not occur. If they do:

```bash
# Check what's using ports
lsof -i :8080
lsof -i :5432
```

## CI/CD Integration

This test is designed for CI/CD pipelines:

```yaml
# GitHub Actions example
- name: Run Integration Tests
  run: mvn test -Dtest=KeycloakLoginIntegrationTest
  env:
    TESTCONTAINERS_RYUK_DISABLED: false
```

## Performance

Typical execution times:
- **First run**: ~2-3 minutes (downloads images)
- **Subsequent runs**: ~15-20 seconds
- **PostgreSQL startup**: ~3-5 seconds
- **Keycloak startup**: ~10-15 seconds
- **Terraform apply**: ~2-3 seconds

## Best Practices

1. **Use `@Container` static fields**: Ensures containers start before `@BeforeAll`
2. **Use `.dependsOn()`**: Ensures correct startup order
3. **Use network aliases**: Allows containers to communicate by name
4. **Use wait strategies**: Don't hardcode sleeps, use proper wait conditions
5. **Clean up Terraform state**: Always destroy in `@AfterAll`

## Extending the Tests

### Add more test cases
```java
@Test
public void testTokenRefresh() {
    // Implement token refresh flow
}

@Test  
public void testClientCredentials() {
    // Test client credentials grant
}
```

### Test custom SPIs
```java
@BeforeAll
public static void setUp() {
    keycloakContainer.withFileSystemBind(
        "./target/my-extension.jar",
        "/opt/keycloak/providers/my-extension.jar"
    );
}
```

### Add more realms/clients
Modify the Terraform configuration in `applyTerraformConfiguration()` to create additional resources.

## Resources

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Keycloak Server Administration](https://www.keycloak.org/docs/latest/server_admin/)
- [OpenTofu Documentation](https://opentofu.org/docs/)
- [OAuth 2.0 Password Grant](https://oauth.net/2/grant-types/password/)
