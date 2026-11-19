package com.example.keycloak.resolver.crypto;

import com.example.keycloak.shared.crypto.IdentityHash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class HmacIdentityHasherTest {

    private final byte[] secretKey = new byte[32];

@Test
void shouldGenerateDifferentHashesForDifferentProviders() {
    Arrays.fill(secretKey, (byte) 1);

    HmacIdentityHasher hasher =
            new HmacIdentityHasher(secretKey, 1);

    IdentityHash diiaHash = hasher.hashExternalSubject(
            "diia",
            "external-user-123"
    );

    IdentityHash bankIdHash = hasher.hashExternalSubject(
            "bankid",
            "external-user-123"
    );

    assertNotEquals(diiaHash.value(), bankIdHash.value());
}

@Test
void shouldGenerateDeterministicExternalSubjectHash() {
    Arrays.fill(secretKey, (byte) 1);

    HmacIdentityHasher hasher =
            new HmacIdentityHasher(secretKey, 1);

    IdentityHash first = hasher.hashExternalSubject(
            "diia",
            "external-user-123"
    );

    IdentityHash second = hasher.hashExternalSubject(
            "diia",
            "external-user-123"
    );

    assertEquals(first.value(), second.value());
    assertEquals(1, first.keyVersion());
}

    @Test
    void shouldGenerateDeterministicHash() {
        Arrays.fill(secretKey, (byte) 1);

        HmacIdentityHasher hasher =
                new HmacIdentityHasher(secretKey, 1);

        IdentityHash first = hasher.hash("test-identity");
        IdentityHash second = hasher.hash("test-identity");

        assertEquals(first.value(), second.value());
        assertEquals(1, first.keyVersion());
    }

    @Test
    void shouldGenerateDifferentHashesForDifferentValues() {
        Arrays.fill(secretKey, (byte) 1);

        HmacIdentityHasher hasher =
                new HmacIdentityHasher(secretKey, 1);

        IdentityHash first = hasher.hash("identity-one");
        IdentityHash second = hasher.hash("identity-two");

        assertNotEquals(first.value(), second.value());
    }

    @Test
    void shouldGenerateSha256LengthHash() {
        Arrays.fill(secretKey, (byte) 1);

        HmacIdentityHasher hasher =
                new HmacIdentityHasher(secretKey, 1);

        IdentityHash result = hasher.hash("test-identity");

        assertEquals(64, result.value().length());
    }

    @Test
    void shouldRejectShortSecretKey() {
        byte[] shortKey = new byte[16];

        assertThrows(
                IllegalArgumentException.class,
                () -> new HmacIdentityHasher(shortKey, 1)
        );
    }

    @Test
    void shouldRejectInvalidKeyVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HmacIdentityHasher(secretKey, 0)
        );
    }

    @Test
    void shouldRejectBlankIdentity() {
        Arrays.fill(secretKey, (byte) 1);

        HmacIdentityHasher hasher =
                new HmacIdentityHasher(secretKey, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> hasher.hash(" ")
        );
    }
}
