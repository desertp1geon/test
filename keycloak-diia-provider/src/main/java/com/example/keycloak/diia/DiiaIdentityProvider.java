
package com.example.keycloak.diia;

import com.example.keycloak.broker.AbstractExternalIdentityProvider;

import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import jakarta.ws.rs.core.Response;

public class DiiaIdentityProvider
        extends AbstractExternalIdentityProvider {

    public DiiaIdentityProvider(
            KeycloakSession session,
            IdentityProviderModel config
    ) {
        super(session, config);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("Diia.Signature authentication is not implemented yet")
                .build();
    }

    /**
     * Legacy token retrieval method.
     */
    @Override
    @Deprecated
    public Response retrieveToken(
            KeycloakSession session,
            FederatedIdentityModel identity
    ) {
        return null;
    }

    /**
     * Token retrieval method with user session context.
     */
    @Override
    public Response retrieveToken(
            KeycloakSession session,
            FederatedIdentityModel identity,
            UserSessionModel userSession,
            UserModel user
    ) {
        return null;
    }
}

