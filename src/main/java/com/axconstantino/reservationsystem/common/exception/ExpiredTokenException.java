package com.axconstantino.reservationsystem.common.exception;

/**
 * Exception thrown when a valid but expired password reset token is provided.
 */
public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) {
        super(message);
    }
}