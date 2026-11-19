package com.example.keycloak.shared.identity;

public enum IdentityProviderType {

    DIA("diia"),
    BANK_ID("bankid");

    private final String alias;

    IdentityProviderType(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public static IdentityProviderType fromAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException(
                    "Identity provider alias cannot be null or blank"
            );
        }

        for (IdentityProviderType type : values()) {
            if (type.alias.equalsIgnoreCase(alias.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported identity provider: " + alias
        );
    }
}
