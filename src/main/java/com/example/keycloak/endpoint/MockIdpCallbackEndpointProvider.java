package com.example.keycloak.endpoint;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

public class MockIdpCallbackEndpointProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public MockIdpCallbackEndpointProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        RealmModel realm = session.getContext().getRealm();
        return new MockIdpCallbackEndpoint(session, realm);
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
