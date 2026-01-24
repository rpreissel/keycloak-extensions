package com.example.keycloak.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using Testcontainers with Keycloak and PostgreSQL.
 * 
 * Test Flow:
 * 1. Starts PostgreSQL container
 * 2. Starts Keycloak container
 * 3. Applies Terraform configuration (realm, client, user)
 * 4. Tests login with password grant
 * 5. Cleans up all resources
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakLoginIntegrationTest {

    private static final String KEYCLOAK_VERSION = "26.5.2";
    private static final String POSTGRES_VERSION = "15-alpine";
    
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-client";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "test123";
    
    private static final Network network = Network.newNetwork();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:" + POSTGRES_VERSION))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("keycloak");
    
    @Container
    private static final GenericContainer<?> keycloak = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION))
            .withNetwork(network)
            .dependsOn(postgres)
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/keycloak")
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "keycloak")
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withEnv("KC_HTTP_ENABLED", "true")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/")
                .forPort(8080)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(5)));
    
    private static String keycloakUrl;
    
    @BeforeAll
    static void setUp() throws Exception {
        keycloakUrl = "http://%s:%d".formatted(keycloak.getHost(), keycloak.getMappedPort(8080));
        System.out.println("✓ Keycloak started at: " + keycloakUrl);
        
        applyTerraformConfiguration();
        Thread.sleep(2000); // Wait for configuration to propagate
    }
    
    @AfterAll
    static void tearDown() throws Exception {
        destroyTerraformConfiguration();
        network.close();
    }
    
    @Test
    @Order(1)
    @DisplayName("Container should be running")
    void testKeycloakIsRunning() {
        assertNotNull(keycloakUrl, "Keycloak URL should be set");
        assertTrue(keycloak.isRunning(), "Keycloak container should be running");
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should authenticate with valid credentials")
    void testPasswordGrantLogin() throws Exception {
        TokenResponse response = requestToken(USERNAME, PASSWORD);
        
        assertEquals(200, response.statusCode, "Should return 200 OK");
        assertNotNull(response.accessToken, "Access token should not be null");
        assertFalse(response.accessToken.isEmpty(), "Access token should not be empty");
        assertEquals("Bearer", response.tokenType, "Token type should be Bearer");
        assertTrue(response.expiresIn > 0, "Expires in should be positive");
        
        System.out.println("✓ Login successful - Token: " + 
            response.accessToken.substring(0, Math.min(50, response.accessToken.length())) + "...");
    }
    
    @Test
    @Order(3)
    @DisplayName("Should reject invalid credentials")
    void testInvalidPasswordLogin() throws Exception {
        TokenResponse response = requestToken(USERNAME, "wrong-password");
        
        assertEquals(401, response.statusCode, "Should return 401 Unauthorized");
        assertNull(response.accessToken, "Access token should be null");
        assertEquals("invalid_grant", response.error, "Error should be 'invalid_grant'");
        
        System.out.println("✓ Invalid credentials correctly rejected");
    }
    
    @Test
    @Order(4)
    @DisplayName("Realm configuration should be accessible")
    void testRealmConfiguration() throws Exception {
        String realmUrl = keycloakUrl + "/realms/" + REALM_NAME;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(realmUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(200, response.getCode(), "Realm should be accessible");
                
                String body = EntityUtils.toString(response.getEntity());
                JsonNode realmInfo = objectMapper.readTree(body);
                
                assertEquals(REALM_NAME, realmInfo.get("realm").asText(), 
                    "Realm name should match");
                
                System.out.println("✓ Realm '" + REALM_NAME + "' verified");
            }
        }
    }
    
    // ==================== Helper Methods ====================
    
    private static TokenResponse requestToken(String username, String password) throws Exception {
        String tokenUrl = keycloakUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(tokenUrl);
            request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("grant_type", "password"),
                new BasicNameValuePair("client_id", CLIENT_ID),
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password),
                new BasicNameValuePair("scope", "openid")
            ), StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity());
                JsonNode json = objectMapper.readTree(body);
                
                return new TokenResponse(
                    statusCode,
                    json.has("access_token") ? json.get("access_token").asText() : null,
                    json.has("token_type") ? json.get("token_type").asText() : null,
                    json.has("expires_in") ? json.get("expires_in").asInt() : 0,
                    json.has("error") ? json.get("error").asText() : null
                );
            }
        }
    }
    
    private static void applyTerraformConfiguration() throws Exception {
        System.out.println("Applying Terraform configuration...");
        Path terraformDir = getTerraformDir();
        Path tfvarsFile = createTfvarsFile(terraformDir);
        
        try {
            runCommand(terraformDir, "tofu", "init");
            runCommand(terraformDir, "tofu", "apply", "-auto-approve");
            System.out.println("✓ Terraform configuration applied");
        } finally {
            Files.deleteIfExists(tfvarsFile);
        }
    }
    
    private static void destroyTerraformConfiguration() throws Exception {
        System.out.println("Destroying Terraform configuration...");
        Path terraformDir = getTerraformDir();
        Path tfvarsFile = createTfvarsFile(terraformDir);
        
        try {
            runCommand(terraformDir, "tofu", "destroy", "-auto-approve");
            System.out.println("✓ Terraform configuration destroyed");
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to destroy Terraform: " + e.getMessage());
        } finally {
            Files.deleteIfExists(tfvarsFile);
        }
    }
    
    private static Path getTerraformDir() {
        Path terraformDir = Paths.get(System.getProperty("user.dir")).resolve("terraform");
        if (!Files.exists(terraformDir)) {
            throw new RuntimeException("Terraform directory not found: " + terraformDir);
        }
        return terraformDir;
    }
    
    private static Path createTfvarsFile(Path terraformDir) throws Exception {
        Path tfvarsFile = terraformDir.resolve("test.auto.tfvars");
        String content = """
            keycloak_url = "%s"
            admin_username = "admin"
            admin_password = "admin"
            realm_name = "%s"
            realm_display_name = "Test Realm"
            client_id = "%s"
            client_name = "Test Client"
            redirect_uris = ["http://localhost:3000/callback"]
            web_origins = ["http://localhost:3000"]
            test_username = "%s"
            test_user_email = "test@example.com"
            test_user_first_name = "Test"
            test_user_last_name = "User"
            test_user_password = "%s"
            """.formatted(keycloakUrl, REALM_NAME, CLIENT_ID, USERNAME, PASSWORD);
        
        Files.writeString(tfvarsFile, content);
        return tfvarsFile;
    }
    
    private static void runCommand(Path workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command)
            .directory(workingDir.toFile())
            .redirectErrorStream(true);
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }
        
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", command));
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Command failed (exit code %d): %s"
                .formatted(process.exitValue(), String.join(" ", command)));
        }
    }
    
    // ==================== Helper Classes ====================
    
    private record TokenResponse(
        int statusCode,
        String accessToken,
        String tokenType,
        int expiresIn,
        String error
    ) {}
}
