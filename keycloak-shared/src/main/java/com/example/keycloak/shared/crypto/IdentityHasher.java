package com.example.keycloak.shared.crypto;

public interface IdentityHasher {

    IdentityHash hash(String identityValue);
    IdentityHash hashExternalSubject(
            String providerAlias,
            String externalSubject
    );
}
