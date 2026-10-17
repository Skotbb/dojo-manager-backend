package com.dojomanager.services.exceptions;

/**
 * Thrown when the authenticated principal is not allowed to perform the
 * requested operation, for example when a supplied dojo/rank id belongs to
 * another owner. Mapped to 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
