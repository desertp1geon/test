package com.example.keycloak.bankid;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class BankIdIdentityProviderFactory
        extends AbstractIdentityProviderFactory<BankIdIdentityProvider> {

    public static final String PROVIDER_ID = "bankid";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "BankID";
    }

    @Override
    public BankIdIdentityProvider create(
            KeycloakSession session,
            IdentityProviderModel model
    ) {
        return new BankIdIdentityProvider(session, model);
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new IdentityProviderModel();
    }
}
