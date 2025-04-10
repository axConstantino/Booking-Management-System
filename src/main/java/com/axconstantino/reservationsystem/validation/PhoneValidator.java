package com.axconstantino.reservationsystem.validation;

import com.axconstantino.reservationsystem.common.exception.InvalidPhoneNumberException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhoneValidator {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "MX";

    public boolean isValidPhoneNumber(String phoneNumberString) {
        if (phoneNumberString == null || phoneNumberString.isBlank()) {
            throw new InvalidPhoneNumberException(phoneNumberString);
        }
        try {
            PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberString, DEFAULT_REGION);
            return phoneUtil.isValidNumber(phoneNumber);
        } catch (NumberParseException e) {
            log.warn("Número '{}' no pudo ser parseado: {}", phoneNumberString, e.getMessage());
            return false;
        }
    }

    public String formatToE164(String phoneNumberString) {
        if (phoneNumberString == null || phoneNumberString.isBlank()) {
            return phoneNumberString;
        }
        try {
            PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberString, DEFAULT_REGION);
            if (phoneUtil.isValidNumber(phoneNumber)) {
                return phoneUtil.format(phoneNumber, PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
            log.warn("Número '{}' no pudo ser parseado para formateo: {}", phoneNumberString, e.getMessage());
        }
        return null;
    }
}
