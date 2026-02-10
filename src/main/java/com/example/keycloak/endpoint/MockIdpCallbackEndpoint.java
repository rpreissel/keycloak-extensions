package com.example.keycloak.endpoint;

import com.example.keycloak.service.MockIdpJwtValidator;
import com.example.keycloak.service.MockIdpSessionCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class MockIdpCallbackEndpoint {

    private static final Logger logger = Logger.getLogger(MockIdpCallbackEndpoint.class);

    private final KeycloakSession session;
    private final RealmModel realm;

    public MockIdpCallbackEndpoint(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    @POST
    @Path("/endpoint")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleCallback(Map<String, String> payload) {
        logger.info("Received Mock IDP callback");

        try {
            // Extract parameters from payload
            String jwt = payload.get("jwt");
            String transactionId = payload.get("transaction_id");
            String callbackToken = payload.get("callback_token");

            if (jwt == null || transactionId == null || callbackToken == null) {
                logger.error("Missing required parameters in callback");
                return errorResponse("Missing required parameters: jwt, transaction_id, or callback_token");
            }

            logger.debugf("Processing callback - transaction_id: %s", transactionId);

            // Find authentication session by transaction ID
            AuthenticationSessionModel authSession = findAuthenticationSession(transactionId);
            if (authSession == null) {
                logger.errorf("Authentication session not found for transaction_id: %s", transactionId);
                return errorResponse("Invalid or expired transaction ID");
            }
            
            // Validate transaction ID matches
            String expectedTransactionId = authSession.getAuthNote("MOCK_IDP_TRANSACTION_ID");
            if (!transactionId.equals(expectedTransactionId)) {
                logger.error("Transaction ID mismatch");
                return errorResponse("Transaction ID mismatch");
            }

            // Validate callback token
            String expectedToken = authSession.getAuthNote("MOCK_IDP_CALLBACK_TOKEN");
            if (!callbackToken.equals(expectedToken)) {
                logger.error("Callback token mismatch");
                return errorResponse("Invalid callback token");
            }

            // Validate JWT
            String clientId = authSession.getClient().getClientId();
            String mockIdpUrl = "http://localhost/mock-idp"; // TODO: Get from config
            
            Map<String, Object> claims = MockIdpJwtValidator.validateJwt(jwt, mockIdpUrl, clientId);
            logger.infof("JWT validated successfully - sub: %s", claims.get("sub"));

            // Store JWT claims in auth session for later processing by authenticator
            ObjectMapper mapper = new ObjectMapper();
            String claimsJson = mapper.writeValueAsString(claims);
            authSession.setAuthNote("MOCK_IDP_JWT_CLAIMS", claimsJson);
            
            // Store received transaction ID for validation in authenticator
            authSession.setAuthNote("MOCK_IDP_RECEIVED_TRANSACTION_ID", transactionId);
            
            // Mark callback as successful
            authSession.setAuthNote("MOCK_IDP_CALLBACK_SUCCESS", "true");

            logger.infof("Mock IDP callback processed successfully - sub: %s", claims.get("sub"));

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction_id", transactionId);
            
            return Response.ok(response).build();

        } catch (Exception e) {
            logger.errorf(e, "Failed to process Mock IDP callback");
            return errorResponse("Callback processing failed: " + e.getMessage());
        }
    }

    /**
     * Find authentication session by transaction ID using SingleUseObjectProvider
     */
    private AuthenticationSessionModel findAuthenticationSession(String transactionId) {
        // Retrieve session data from SingleUseObjectProvider and remove it (single-use)
        MockIdpSessionCache.SessionData sessionData = MockIdpSessionCache.remove(session, transactionId);
        
        if (sessionData == null) {
            logger.warnf("No cached session data found for transaction_id: %s", transactionId);
            return null;
        }
        
        logger.debugf("Found session data - authSessionId: %s, tabId: %s, clientUuid: %s", 
                     sessionData.authSessionId, sessionData.tabId, sessionData.clientUuid);
        
        // Retrieve the root authentication session
        var rootSession = session.authenticationSessions()
            .getRootAuthenticationSession(realm, sessionData.authSessionId);
            
        if (rootSession == null) {
            logger.warnf("Root authentication session not found - authSessionId: %s", sessionData.authSessionId);
            return null;
        }
        
        // Get client model
        var client = realm.getClientById(sessionData.clientUuid);
        if (client == null) {
            logger.warnf("Client not found - clientUuid: %s", sessionData.clientUuid);
            return null;
        }
        
        // Get authentication session
        AuthenticationSessionModel authSession = rootSession.getAuthenticationSession(client, sessionData.tabId);
        
        if (authSession == null) {
            logger.warnf("Authentication session not found - tabId: %s", sessionData.tabId);
        }
        
        return authSession;
    }

    /**
     * Create error response
     */
    private Response errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "callback_failed");
        error.put("error_description", message);
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
