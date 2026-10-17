package com.dojomanager.services.exceptions;

/**
 * Thrown when a payload is well formed but conflicts with existing data
 * (for example a student email that is already used in the same dojo).
 * Mapped to 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
