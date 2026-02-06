package com.example.keycloak.service;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

/**
 * Service for managing users authenticated via Mock IDP.
 * Handles user creation, updates, and attribute mapping from JWT claims.
 */
public class MockIdpUserService {

    private static final Logger logger = Logger.getLogger(MockIdpUserService.class);

    private final KeycloakSession session;
    private final RealmModel realm;

    public MockIdpUserService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    /**
     * Create or update user with claims from JWT.
     * This method is idempotent - it will create the user if they don't exist,
     * or update them if they do.
     * 
     * @param claims JWT claims from Mock IDP
     * @return Created or updated user
     */
    public UserModel createOrUpdateUser(Map<String, Object> claims) {
        // Log all received claims for debugging
        logger.infof("Received JWT claims: %s", claims.toString());
        
        String userId = (String) claims.get("sub");
        String givenName = (String) claims.get("given_name");
        String familyName = (String) claims.get("family_name");
        String name = (String) claims.get("name");
        String email = (String) claims.get("email");
        String birthdate = (String) claims.get("birthdate");
        String verificationStatus = (String) claims.get("verification_status");

        logger.infof("Extracted from claims - email: %s, given_name: %s, family_name: %s", 
                     email, givenName, familyName);

        // Generate username from user ID
        String username = "mock-idp-" + userId;

        // Check if user already exists
        UserModel user = session.users().getUserByUsername(realm, username);
        
        if (user == null) {
            // Create new user
            logger.infof("Creating new user: %s", username);
            user = session.users().addUser(realm, username);
            user.setEnabled(true);
        } else {
            logger.infof("Updating existing user: %s", username);
        }

        // Set user attributes
        if (givenName != null) {
            user.setFirstName(givenName);
            logger.infof("Set firstName: %s", givenName);
        }
        if (familyName != null) {
            user.setLastName(familyName);
            logger.infof("Set lastName: %s", familyName);
        }
        if (email != null) {
            user.setEmail(email);
            user.setEmailVerified(true); // Email from IDP is considered verified
            logger.infof("Set email: %s (verified)", email);
        } else {
            logger.warn("Email is NULL in JWT claims!");
        }
        if (birthdate != null) {
            user.setSingleAttribute("birthdate", birthdate);
        }
        if (verificationStatus != null) {
            user.setSingleAttribute("verification_status", verificationStatus);
        }
        
        // Store original Mock IDP user ID
        user.setSingleAttribute("mock_idp_user_id", userId);

        // Store full name if available
        if (name != null) {
            user.setSingleAttribute("full_name", name);
        }

        logger.debugf("User updated - username: %s, email: %s, verification_status: %s", 
                     username, email, verificationStatus);

        return user;
    }

    /**
     * Find user by Mock IDP user ID.
     * 
     * @param mockIdpUserId Mock IDP user ID (sub claim)
     * @return User if found, null otherwise
     */
    public UserModel findUserByMockIdpUserId(String mockIdpUserId) {
        String username = "mock-idp-" + mockIdpUserId;
        return session.users().getUserByUsername(realm, username);
    }
}
