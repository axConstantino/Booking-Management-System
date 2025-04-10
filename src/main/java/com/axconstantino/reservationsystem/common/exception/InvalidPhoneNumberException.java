package com.axconstantino.reservationsystem.common.exception;

public class InvalidPhoneNumberException extends RuntimeException {
    public InvalidPhoneNumberException(String phoneNumber) {
        super("Invalid phone number: " + phoneNumber);
    }
}
