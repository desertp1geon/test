package com.example.keycloak.shared.exception;

public class IdentityHashingException extends RuntimeException {

    public IdentityHashingException(String message) {
        super(message);
    }

    public IdentityHashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
