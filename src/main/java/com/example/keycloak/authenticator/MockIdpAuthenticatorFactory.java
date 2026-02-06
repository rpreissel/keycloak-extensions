package com.example.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class MockIdpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "mock-idp-authenticator";
    public static final String CONFIG_MOCK_IDP_URL = "mock.idp.url";
    
    private static final MockIdpAuthenticator SINGLETON = new MockIdpAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Mock Identity Provider";
    }

    @Override
    public String getHelpText() {
        return "Redirects users to a mock identity provider for testing identity verification flows.";
    }

    @Override
    public String getReferenceCategory() {
        return "mock-idp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_MOCK_IDP_URL)
                    .label("Mock IDP URL")
                    .helpText("The base URL of the Mock Identity Provider service")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("http://localhost:3001")
                    .add()
                .build();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
