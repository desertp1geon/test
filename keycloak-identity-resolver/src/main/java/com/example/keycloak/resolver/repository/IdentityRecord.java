package com.example.keycloak.resolver.repository;

import java.util.UUID;

public record IdentityRecord(
        UUID id,
        String keycloakUserId
) {
}
