package com.axconstantino.reservationsystem.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Object id) {
        super("Entity with id=" + id + " not found");
    }
}
