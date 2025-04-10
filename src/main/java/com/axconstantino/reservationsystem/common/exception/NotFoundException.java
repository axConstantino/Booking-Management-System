package com.axconstantino.reservationsystem.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(Class<?> entityClass, Object id) {
        super(entityClass.getSimpleName() + " with id=" + id + " not found");
    }

    public NotFoundException(String email) {
        super("User with email " + email + " not found");
    }
}
