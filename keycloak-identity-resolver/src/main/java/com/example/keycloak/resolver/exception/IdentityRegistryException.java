package com.example.keycloak.resolver.exception;

public class IdentityRegistryException extends RuntimeException {

    public IdentityRegistryException(String message) {
        super(message);
    }

    public IdentityRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
