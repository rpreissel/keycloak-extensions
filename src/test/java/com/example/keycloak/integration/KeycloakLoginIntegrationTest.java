package com.example.keycloak.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using Testcontainers with Keycloak and PostgreSQL.
 * This test:
 * 1. Starts PostgreSQL container
 * 2. Starts Keycloak container
 * 3. Applies Terraform configuration (realm, client, user)
 * 4. Tests login with password grant
 */
@Testcontainers
public class KeycloakLoginIntegrationTest {

    private static final String KEYCLOAK_VERSION = "26.5.2";
    private static final String POSTGRES_VERSION = "15-alpine";
    
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-client";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "test123";
    
    private static Network network = Network.newNetwork();
    
    @Container
    private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + POSTGRES_VERSION))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("keycloak");
    
    @Container
    private static GenericContainer<?> keycloakContainer = new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION))
            .withNetwork(network)
            .dependsOn(postgresContainer)
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/keycloak")
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "keycloak")
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withEnv("KC_METRICS_ENABLED", "true")
            .withEnv("KC_HTTP_ENABLED", "true")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/")
                .forPort(8080)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(5)));
    
    private static String keycloakUrl;
    private static ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeAll
    public static void setUp() throws Exception {
        // Get Keycloak URL
        keycloakUrl = String.format("http://%s:%d", 
            keycloakContainer.getHost(), 
            keycloakContainer.getMappedPort(8080));
        
        System.out.println("Keycloak started at: " + keycloakUrl);
        
        // Apply Terraform configuration
        applyTerraformConfiguration();
        
        // Wait a bit for configuration to be fully applied
        Thread.sleep(2000);
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        // Destroy Terraform configuration
        destroyTerraformConfiguration();
        
        // Containers will be automatically stopped by Testcontainers
        if (network != null) {
            network.close();
        }
    }
    
    /**
     * Apply Terraform configuration to set up realm, client, and user
     */
    private static void applyTerraformConfiguration() throws Exception {
        System.out.println("Applying Terraform configuration...");
        
        // Get project root
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path terraformDir = projectRoot.resolve("terraform");
        
        if (!Files.exists(terraformDir)) {
            throw new RuntimeException("Terraform directory not found: " + terraformDir);
        }
        
        // Create temporary tfvars file with dynamic Keycloak URL
        Path tfvarsFile = terraformDir.resolve("test.auto.tfvars");
        String tfvarsContent = String.format(
            "keycloak_url = \"%s\"\n" +
            "admin_username = \"admin\"\n" +
            "admin_password = \"admin\"\n" +
            "realm_name = \"%s\"\n" +
            "realm_display_name = \"Test Realm\"\n" +
            "client_id = \"%s\"\n" +
            "client_name = \"Test Client\"\n" +
            "redirect_uris = [\"http://localhost:3000/callback\"]\n" +
            "web_origins = [\"http://localhost:3000\"]\n" +
            "test_username = \"%s\"\n" +
            "test_user_email = \"test@example.com\"\n" +
            "test_user_first_name = \"Test\"\n" +
            "test_user_last_name = \"User\"\n" +
            "test_user_password = \"%s\"\n",
            keycloakUrl, REALM_NAME, CLIENT_ID, USERNAME, PASSWORD
        );
        Files.writeString(tfvarsFile, tfvarsContent);
        
        try {
            // Initialize Terraform
            runCommand(terraformDir, "tofu", "init");
            
            // Apply configuration
            runCommand(terraformDir, "tofu", "apply", "-auto-approve");
            
            System.out.println("Terraform configuration applied successfully");
        } finally {
            // Clean up tfvars file
            Files.deleteIfExists(tfvarsFile);
        }
    }
    
    /**
     * Destroy Terraform configuration
     */
    private static void destroyTerraformConfiguration() throws Exception {
        System.out.println("Destroying Terraform configuration...");
        
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path terraformDir = projectRoot.resolve("terraform");
        
        if (!Files.exists(terraformDir)) {
            return;
        }
        
        // Create temporary tfvars file
        Path tfvarsFile = terraformDir.resolve("test.auto.tfvars");
        String tfvarsContent = String.format(
            "keycloak_url = \"%s\"\n" +
            "admin_username = \"admin\"\n" +
            "admin_password = \"admin\"\n" +
            "realm_name = \"%s\"\n" +
            "realm_display_name = \"Test Realm\"\n" +
            "client_id = \"%s\"\n" +
            "client_name = \"Test Client\"\n" +
            "redirect_uris = [\"http://localhost:3000/callback\"]\n" +
            "web_origins = [\"http://localhost:3000\"]\n" +
            "test_username = \"%s\"\n" +
            "test_user_email = \"test@example.com\"\n" +
            "test_user_first_name = \"Test\"\n" +
            "test_user_last_name = \"User\"\n" +
            "test_user_password = \"%s\"\n",
            keycloakUrl, REALM_NAME, CLIENT_ID, USERNAME, PASSWORD
        );
        Files.writeString(tfvarsFile, tfvarsContent);
        
        try {
            runCommand(terraformDir, "tofu", "destroy", "-auto-approve");
            System.out.println("Terraform configuration destroyed");
        } catch (Exception e) {
            System.err.println("Warning: Failed to destroy Terraform configuration: " + e.getMessage());
        } finally {
            Files.deleteIfExists(tfvarsFile);
        }
    }
    
    /**
     * Run a command and wait for completion
     */
    private static void runCommand(Path workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        
        System.out.println("Running: " + String.join(" ", command));
        
        Process process = pb.start();
        
        // Read output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", command));
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Command failed with exit code " + process.exitValue() + 
                ": " + String.join(" ", command));
        }
    }
    
    @Test
    public void testKeycloakIsRunning() {
        assertNotNull(keycloakUrl);
        assertTrue(keycloakContainer.isRunning());
    }
    
    @Test
    public void testPasswordGrantLogin() throws Exception {
        // Perform password grant (direct grant)
        String tokenUrl = keycloakUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenUrl);
            
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("client_id", CLIENT_ID));
            params.add(new BasicNameValuePair("username", USERNAME));
            params.add(new BasicNameValuePair("password", PASSWORD));
            params.add(new BasicNameValuePair("scope", "openid"));
            
            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                System.out.println("Token response status: " + statusCode);
                System.out.println("Token response body: " + responseBody);
                
                assertEquals(200, statusCode, "Expected successful token response");
                
                // Parse response
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                // Verify token fields
                assertTrue(jsonResponse.has("access_token"), "Response should contain access_token");
                assertTrue(jsonResponse.has("token_type"), "Response should contain token_type");
                assertTrue(jsonResponse.has("expires_in"), "Response should contain expires_in");
                
                String accessToken = jsonResponse.get("access_token").asText();
                assertNotNull(accessToken);
                assertFalse(accessToken.isEmpty());
                
                // Verify token type
                assertEquals("Bearer", jsonResponse.get("token_type").asText());
                
                System.out.println("✅ Login successful!");
                System.out.println("Access Token (first 50 chars): " + accessToken.substring(0, Math.min(50, accessToken.length())) + "...");
            }
        }
    }
    
    @Test
    public void testInvalidPasswordLogin() throws Exception {
        String tokenUrl = keycloakUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenUrl);
            
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("client_id", CLIENT_ID));
            params.add(new BasicNameValuePair("username", USERNAME));
            params.add(new BasicNameValuePair("password", "wrong-password"));
            params.add(new BasicNameValuePair("scope", "openid"));
            
            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                System.out.println("Invalid login status: " + statusCode);
                System.out.println("Invalid login response: " + responseBody);
                
                assertEquals(401, statusCode, "Expected unauthorized response for invalid password");
                
                // Parse error response
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                assertTrue(jsonResponse.has("error"), "Error response should contain 'error' field");
                
                String error = jsonResponse.get("error").asText();
                assertEquals("invalid_grant", error, "Error should be 'invalid_grant'");
                
                System.out.println("✅ Invalid password correctly rejected");
            }
        }
    }
    
    @Test
    public void testRealmConfiguration() throws Exception {
        // Get realm configuration
        String realmUrl = keycloakUrl + "/realms/" + REALM_NAME;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            org.apache.hc.client5.http.classic.methods.HttpGet httpGet = 
                new org.apache.hc.client5.http.classic.methods.HttpGet(realmUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                assertEquals(200, statusCode, "Realm should be accessible");
                
                JsonNode realmInfo = objectMapper.readTree(responseBody);
                
                assertEquals(REALM_NAME, realmInfo.get("realm").asText(), "Realm name should match");
                
                System.out.println("✅ Realm configuration verified");
                System.out.println("Realm: " + realmInfo.get("realm").asText());
            }
        }
    }
}
