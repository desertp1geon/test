package com.example.keycloak.resolver.crypto;

import com.example.keycloak.shared.crypto.IdentityHash;
import com.example.keycloak.shared.crypto.IdentityHasher;
import com.example.keycloak.shared.exception.IdentityHashingException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

import java.util.Locale;

public final class HmacIdentityHasher implements IdentityHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String IDENTITY_DOMAIN =
        "identity:v1:";

    private static final String EXTERNAL_SUBJECT_DOMAIN =
        "external-subject:v1:";
    private static final String DOMAIN_SEPARATOR = "identity:v1:";

    private final byte[] secretKey;
    private final int keyVersion;

    public HmacIdentityHasher(byte[] secretKey, int keyVersion) {
        if (secretKey == null || secretKey.length < 32) {
            throw new IllegalArgumentException(
                    "HMAC secret must contain at least 32 bytes"
            );
        }

        if (keyVersion < 1) {
            throw new IllegalArgumentException(
                    "Key version must be greater than zero"
            );
        }

        this.secretKey = secretKey.clone();
        this.keyVersion = keyVersion;
    }

    public static HmacIdentityHasher fromEnvironment() {
        String encodedKey = System.getenv("IDENTITY_HMAC_KEY_BASE64");
        String versionValue = System.getenv("IDENTITY_HMAC_KEY_VERSION");

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing IDENTITY_HMAC_KEY_BASE64"
            );
        }

        if (versionValue == null || versionValue.isBlank()) {
            throw new IllegalStateException(
                    "Missing IDENTITY_HMAC_KEY_VERSION"
            );
        }

        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            int version = Integer.parseInt(versionValue);

            return new HmacIdentityHasher(key, version);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid HMAC environment configuration",
                    exception
            );
        }
    }

@Override
public IdentityHash hash(String identityValue) {
    if (identityValue == null || identityValue.isBlank()) {
        throw new IllegalArgumentException(
                "Identity value cannot be null or blank"
        );
    }

    return hashWithDomain(IDENTITY_DOMAIN, identityValue);
}

@Override
public IdentityHash hashExternalSubject(
        String providerAlias,
        String externalSubject
) {
    if (providerAlias == null || providerAlias.isBlank()) {
        throw new IllegalArgumentException(
                "Provider alias cannot be null or blank"
        );
    }

    if (externalSubject == null || externalSubject.isBlank()) {
        throw new IllegalArgumentException(
                "External subject cannot be null or blank"
        );
    }

    String normalizedProvider = providerAlias.trim().toLowerCase(Locale.ROOT);

    return hashWithDomain(
            EXTERNAL_SUBJECT_DOMAIN + normalizedProvider + ":",
            externalSubject
    );
}

private IdentityHash hashWithDomain(
        String domain,
        String value
) {
    try {
        Mac mac = Mac.getInstance(ALGORITHM);

        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey,
                ALGORITHM
        );

        mac.init(keySpec);

        byte[] input = (
                domain + value.trim()
        ).getBytes(StandardCharsets.UTF_8);

        byte[] digest = mac.doFinal(input);

        return new IdentityHash(
                HexFormat.of().formatHex(digest),
                keyVersion
        );

    } catch (Exception exception) {
        throw new IdentityHashingException(
                "Unable to calculate identity HMAC",
                exception
        );
    }
} 
}
