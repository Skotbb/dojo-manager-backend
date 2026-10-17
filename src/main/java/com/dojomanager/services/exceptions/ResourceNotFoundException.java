package com.dojomanager.services.exceptions;

/**
 * Thrown when a caller asks for a resource that does not exist <em>within the
 * caller's own scope</em>. Mapped to 404 so a missing resource and somebody
 * else's resource are indistinguishable to the caller.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
