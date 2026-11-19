package com.example.keycloak.resolver.config;

public record IdentityRegistryConfig(
        String jdbcUrl,
        String username,
        String password
) {

    public IdentityRegistryConfig {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Registry JDBC URL is required"
            );
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Registry database username is required"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Registry database password is required"
            );
        }
    }

    public static IdentityRegistryConfig fromEnvironment() {
        return new IdentityRegistryConfig(
                requiredEnv("REGISTRY_DB_URL"),
                requiredEnv("REGISTRY_DB_USER"),
                requiredEnv("REGISTRY_DB_PASSWORD")
        );
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + name
            );
        }

        return value;
    }
}
