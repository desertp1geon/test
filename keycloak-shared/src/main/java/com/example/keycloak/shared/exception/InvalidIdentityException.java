package com.example.keycloak.shared.exception;

public class InvalidIdentityException extends RuntimeException {

    public InvalidIdentityException(String message) {
        super(message);
    }

    public InvalidIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
