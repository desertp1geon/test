package com.example.keycloak.resolver.repository;

import java.util.UUID;

public record IdentityRegistrationResult(
        UUID identityRecordId,
        String keycloakUserId,
        boolean identityCreated,
        boolean providerLinkCreated
) {
}
