package com.example.keycloak.resolver.repository;

import com.example.keycloak.shared.crypto.IdentityHash;
import com.example.keycloak.shared.identity.IdentityProviderType;

import java.util.Optional;

public interface IdentityRecordRepository {

    Optional<IdentityRecord> findByIdentityHash(
            IdentityHash identityHash
    );

    IdentityRegistrationResult register(
            IdentityHash identityHash,
            IdentityHash externalSubjectHash,
            IdentityProviderType providerType,
            String keycloakUserId
    );
}
