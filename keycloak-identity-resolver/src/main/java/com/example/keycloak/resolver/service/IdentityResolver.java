package com.example.keycloak.resolver.service;

import com.example.keycloak.resolver.repository.IdentityRecord;
import com.example.keycloak.resolver.repository.IdentityRecordRepository;
import com.example.keycloak.resolver.repository.IdentityRegistrationResult;
import com.example.keycloak.shared.crypto.IdentityHash;
import com.example.keycloak.shared.crypto.IdentityHasher;
import com.example.keycloak.shared.identity.IdentityMatchResult;
import com.example.keycloak.shared.identity.VerifiedIdentity;

import java.util.Optional;

public final class IdentityResolver {

    private final IdentityHasher identityHasher;
    private final IdentityRecordRepository identityRecordRepository;

    public IdentityResolver(
            IdentityHasher identityHasher,
            IdentityRecordRepository identityRecordRepository
    ) {
        this.identityHasher = identityHasher;
        this.identityRecordRepository = identityRecordRepository;
    }

    public IdentityMatchResult resolve(VerifiedIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException(
                    "Verified identity cannot be null"
            );
        }

        IdentityHash identityHash = identityHasher.hash(
                identity.identityValue()
        );

        Optional<IdentityRecord> record =
                identityRecordRepository.findByIdentityHash(identityHash);

        if (record.isEmpty()) {
            return IdentityMatchResult.notFound();
        }

        IdentityRecord matchedRecord = record.get();

        return IdentityMatchResult.matched(
                matchedRecord.id(),
                matchedRecord.keycloakUserId()
        );
    }

    public IdentityRegistrationResult register(
            VerifiedIdentity identity,
            String keycloakUserId
    ) {
        if (identity == null) {
            throw new IllegalArgumentException(
                    "Verified identity cannot be null"
            );
        }

        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "Keycloak user ID is required"
            );
        }

        IdentityHash identityHash = identityHasher.hash(
                identity.identityValue()
        );

        IdentityHash externalSubjectHash =
                identityHasher.hashExternalSubject(
                        identity.providerType().getAlias(),
                        identity.externalSubject()
                );

        return identityRecordRepository.register(
                identityHash,
                externalSubjectHash,
                identity.providerType(),
                keycloakUserId
        );
    }
}
