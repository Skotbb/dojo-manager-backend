package com.dojomanager.services.exceptions;

/**
 * Raised when a request is malformed - for example when a check-in is missing
 * the {@code sessionId}/{@code date} combination the service requires.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
