package com.example.keycloak.shared.crypto;

import java.util.Objects;

public record IdentityHash(
        String value,
        int keyVersion
) {

    public IdentityHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Hash value cannot be null or blank"
            );
        }

        if (keyVersion < 1) {
            throw new IllegalArgumentException(
                    "Key version must be greater than zero"
            );
        }

        value = value.trim();
    }
}
