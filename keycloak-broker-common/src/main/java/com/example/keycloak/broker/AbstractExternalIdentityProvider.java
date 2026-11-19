
package com.example.keycloak.broker;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractExternalIdentityProvider
        extends AbstractIdentityProvider<IdentityProviderModel> {

    protected AbstractExternalIdentityProvider(
            KeycloakSession session,
            IdentityProviderModel config
    ) {
        super(session, config);
    }

    protected String getRequiredConfig(String key) {
        String value = getConfig().getConfig().get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing identity provider configuration: " + key
            );
        }

        return value.trim();
    }
}

