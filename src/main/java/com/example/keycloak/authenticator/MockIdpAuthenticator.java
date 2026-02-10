package com.example.keycloak.authenticator;

import com.example.keycloak.service.MockIdpJwtValidator;
import com.example.keycloak.service.MockIdpSessionCache;
import com.example.keycloak.service.MockIdpUserService;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class MockIdpAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(MockIdpAuthenticator.class);

    // Session note keys
    private static final String MOCK_IDP_TRANSACTION_ID = "MOCK_IDP_TRANSACTION_ID";
    private static final String MOCK_IDP_CALLBACK_TOKEN = "MOCK_IDP_CALLBACK_TOKEN";
    private static final String MOCK_IDP_STATE = "MOCK_IDP_STATE";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String sessionId = authSession.getParentSession().getId();
        
        logger.infof("========================================");
        logger.infof("Mock IDP Authenticator - START");
        logger.infof("Session ID: %s", sessionId);
        logger.infof("Client: %s", authSession.getClient().getClientId());
        
        // Check if user is already authenticated (should not happen if cookie auth succeeded)
        UserModel existingUser = authSession.getAuthenticatedUser();
        if (existingUser != null) {
            logger.warnf("⚠️  User already authenticated: %s (username: %s)", 
                        existingUser.getId(), existingUser.getUsername());
            logger.warn("⚠️  This should NOT happen if cookie authenticator succeeded!");
            logger.warn("⚠️  Cookie authenticator likely returned ATTEMPTED instead of SUCCESS");
        } else {
            logger.info("✓ No authenticated user yet - proceeding with Mock IDP authentication");
        }
        
        // Check if callback has already completed successfully
        String callbackSuccess = authSession.getAuthNote("MOCK_IDP_CALLBACK_SUCCESS");
        
        if ("true".equals(callbackSuccess)) {
            logger.info("✓ Mock IDP callback already completed - completing authentication");
            // Complete authentication with user creation
            completeAuthentication(context);
            logger.infof("Mock IDP Authenticator - END (callback completed)");
            logger.infof("========================================");
            return;
        }
        
        logger.info("→ No callback yet - redirecting to Mock IDP");

        // Generate transaction ID and callback token
        String transactionId = UUID.randomUUID().toString();
        String callbackToken = UUID.randomUUID().toString();
        String state = context.getAuthenticationSession().getParentSession().getId();

        // Store in authentication session for later validation
        authSession.setAuthNote(MOCK_IDP_TRANSACTION_ID, transactionId);
        authSession.setAuthNote(MOCK_IDP_CALLBACK_TOKEN, callbackToken);
        authSession.setAuthNote(MOCK_IDP_STATE, state);

        // Cache session data using SingleUseObjectProvider
        MockIdpSessionCache.SessionData sessionData = new MockIdpSessionCache.SessionData(
            authSession.getTabId(),
            authSession.getClient().getId(),
            authSession.getParentSession().getId(),
            callbackToken
        );
        MockIdpSessionCache.put(context.getSession(), transactionId, sessionData);

        logger.debugf("Transaction ID: %s, State: %s", transactionId, state);
        logger.debugf("Callback token: %s", callbackToken);

        // Get Mock IDP URL from authenticator config (or use default)
        String mockIdpUrl = getConfigValue(context, MockIdpAuthenticatorFactory.CONFIG_MOCK_IDP_URL, "http://localhost/mock-idp");
        String clientId = context.getAuthenticationSession().getClient().getClientId();

        // Build callback URL (where Mock IDP will POST the JWT)
        String callbackUrl = buildCallbackUrl(context);
        
        // Build action URL (where user's browser should be redirected after Mock IDP)
        String actionUrl = context.getActionUrl("").toString();

        // Build redirect URL to Mock IDP
        String redirectUrl = buildMockIdpRedirectUrl(
                mockIdpUrl,
                clientId,
                transactionId,
                callbackToken,
                state,
                callbackUrl,
                actionUrl
        );

        logger.infof("→ Redirecting to Mock IDP: %s", mockIdpUrl);
        logger.infof("Mock IDP Authenticator - END (redirecting)");
        logger.infof("========================================");

        // Redirect user to Mock IDP
        Response response = Response.seeOther(URI.create(redirectUrl)).build();
        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.infof("========================================");
        logger.infof("Mock IDP Authenticator - ACTION (user returned from Mock IDP)");
        logger.infof("Session ID: %s", context.getAuthenticationSession().getParentSession().getId());
        // This method is called after the user returns from Mock IDP
        completeAuthentication(context);
        logger.infof("Mock IDP Authenticator - ACTION END");
        logger.infof("========================================");
    }
    
    /**
     * Complete authentication by validating transaction ID, creating user, and setting authenticated user
     */
    private void completeAuthentication(AuthenticationFlowContext context) {
        logger.info("→ Completing authentication...");
        
        // We need to:
        // 1. Validate transaction ID
        // 2. Validate JWT and extract claims
        // 3. Create or update user
        // 4. Set authenticated user in session
        
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        
        try {
            // Check if callback was successful
            String callbackSuccess = authSession.getAuthNote("MOCK_IDP_CALLBACK_SUCCESS");
            if (!"true".equals(callbackSuccess)) {
                logger.error("✗ Mock IDP authentication failed - callback not successful");
                context.failure(org.keycloak.authentication.AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
                return;
            }
            logger.info("✓ Callback was successful");

            // Get transaction ID and validate it
            String transactionId = authSession.getAuthNote("MOCK_IDP_TRANSACTION_ID");
            String receivedTransactionId = authSession.getAuthNote("MOCK_IDP_RECEIVED_TRANSACTION_ID");
            
            if (transactionId == null || !transactionId.equals(receivedTransactionId)) {
                logger.errorf("✗ Transaction ID mismatch - expected: %s, received: %s", 
                             transactionId, receivedTransactionId);
                context.failure(org.keycloak.authentication.AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
                return;
            }
            
            logger.debugf("✓ Transaction ID validated: %s", transactionId);

            // Get validated JWT claims from auth session
            String jwtClaimsJson = authSession.getAuthNote("MOCK_IDP_JWT_CLAIMS");
            if (jwtClaimsJson == null) {
                logger.error("✗ JWT claims not found in session");
                context.failure(org.keycloak.authentication.AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
                return;
            }
            logger.info("✓ JWT claims found");

            // Parse JWT claims (stored as JSON)
            Map<String, Object> claims = parseJwtClaims(jwtClaimsJson);
            String subject = (String) claims.get("sub");
            logger.infof("✓ JWT claims parsed - subject: %s", subject);
            
            // Create or update user using service
            MockIdpUserService userService = new MockIdpUserService(
                context.getSession(), 
                context.getRealm()
            );
            UserModel user = userService.createOrUpdateUser(claims);
            logger.infof("✓ User created/updated: %s", user.getUsername());
            
            // Set user in authentication session
            authSession.setAuthenticatedUser(user);
            
            logger.infof("✓ Mock IDP authentication successful for user: %s", user.getUsername());
            context.success();
            
        } catch (Exception e) {
            logger.errorf(e, "✗ Failed to complete Mock IDP authentication");
            context.failure(org.keycloak.authentication.AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
    }
    
    /**
     * Parse JWT claims from JSON string stored in auth session
     */
    private Map<String, Object> parseJwtClaims(String jwtClaimsJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(jwtClaimsJson, Map.class);
        } catch (Exception e) {
            logger.errorf(e, "Failed to parse JWT claims from session");
            throw new RuntimeException("Failed to parse JWT claims", e);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // Nothing to close
    }

    /**
     * Build the callback URL where Mock IDP will POST the JWT
     * This must use the internal container hostname, not the external URL
     */
    private String buildCallbackUrl(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        String realmName = realm.getName();
        
        // Use internal container hostname for container-to-container communication
        // The Mock IDP container will POST to this URL
        return String.format("http://keycloak-dev:8080/realms/%s/mock-idp-callback/endpoint",
                realmName);
    }

    /**
     * Build the redirect URL to Mock IDP with all required parameters
     */
    private String buildMockIdpRedirectUrl(
            String mockIdpUrl,
            String clientId,
            String transactionId,
            String callbackToken,
            String state,
            String callbackUrl,
            String actionUrl) {

        try {
            return String.format(
                    "%s/verify?client_id=%s&transaction_id=%s&callback_token=%s&state=%s&callback_url=%s&redirect_uri=%s",
                    mockIdpUrl,
                    URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                    URLEncoder.encode(transactionId, StandardCharsets.UTF_8),
                    URLEncoder.encode(callbackToken, StandardCharsets.UTF_8),
                    URLEncoder.encode(state, StandardCharsets.UTF_8),
                    URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8),
                    URLEncoder.encode(actionUrl, StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            logger.errorf(e, "Failed to build Mock IDP redirect URL");
            throw new RuntimeException("Failed to build Mock IDP redirect URL", e);
        }
    }

    /**
     * Get configuration value from authenticator config
     */
    private String getConfigValue(AuthenticationFlowContext context, String key, String defaultValue) {
        if (context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig() != null) {
            return context.getAuthenticatorConfig().getConfig().getOrDefault(key, defaultValue);
        }
        return defaultValue;
    }
}
