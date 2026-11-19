package com.example.keycloak.shared.identity;

import java.time.Instant;
import java.util.Objects;

public record VerifiedIdentity(
        IdentityProviderType providerType,
        String externalSubject,
        String identityValue,
        Instant verifiedAt
) {

    public VerifiedIdentity {
        Objects.requireNonNull(providerType, "Provider type is required");
        Objects.requireNonNull(verifiedAt, "Verification timestamp is required");

        if (externalSubject == null || externalSubject.isBlank()) {
            throw new IllegalArgumentException(
                    "External subject cannot be null or blank"
            );
        }

        if (identityValue == null || identityValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Identity value cannot be null or blank"
            );
        }

        externalSubject = externalSubject.trim();
        identityValue = identityValue.trim();
    }
}
