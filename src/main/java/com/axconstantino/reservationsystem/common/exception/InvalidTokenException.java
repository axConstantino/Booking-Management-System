package com.axconstantino.reservationsystem.common.exception;

/**
 * Exception thrown when an invalid password reset token is provided.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}