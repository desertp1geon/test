package com.example.keycloak.diia;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class DiiaIdentityProviderFactory
        extends AbstractIdentityProviderFactory<DiiaIdentityProvider> {

    public static final String PROVIDER_ID = "diia";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Diia.Signature";
    }

    @Override
    public DiiaIdentityProvider create(
            KeycloakSession session,
            IdentityProviderModel model
    ) {
        return new DiiaIdentityProvider(session, model);
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new IdentityProviderModel();
    }
}
