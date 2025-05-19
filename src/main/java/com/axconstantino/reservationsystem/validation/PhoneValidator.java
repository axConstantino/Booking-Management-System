package com.axconstantino.reservationsystem.validation;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for validating and formatting phone numbers using Google's libphonenumber library.
 * <p>
 * This class provides methods to validate phone numbers and to format them into E.164 standard.
 * It defaults to the "MX" region (Mexico) if no region is specified.
 * </p>
 */
@Component
@Slf4j
public class PhoneValidator {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "MX";

    /**
     * Validates if the given phone number string is a valid phone number.
     *
     * @param phoneNumberString the phone number as a string to validate
     * @return true if the phone number is valid, false otherwise
     */
    public boolean isValidPhoneNumber(String phoneNumberString) {
        if (phoneNumberString == null || phoneNumberString.isBlank()) {
            log.info("Validation failed: phone number string is null or blank.");
            return false;
        }
        try {
            PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberString, DEFAULT_REGION);
            boolean isValid = phoneUtil.isValidNumber(phoneNumber);
            log.info("Validation result for number '{}': {}", phoneNumberString, isValid);
            return isValid;
        } catch (NumberParseException e) {
            log.warn("Failed to parse phone number '{}': {}", phoneNumberString, e.getMessage());
            return false;
        }
    }

    /**
     * Formats the given phone number string to E.164 format.
     *
     * @param phoneNumberString the phone number as a string to format
     * @return the phone number formatted in E.164 format if valid; null if invalid or parsing fails;
     * returns the original string if null or blank
     */
    public String formatToE164(String phoneNumberString) {
        if (phoneNumberString == null || phoneNumberString.isBlank()) {
            log.info("Format request received for null or blank phone number string.");
            return phoneNumberString;
        }
        try {
            PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberString, DEFAULT_REGION);
            if (phoneUtil.isValidNumber(phoneNumber)) {
                String formattedNumber = phoneUtil.format(phoneNumber, PhoneNumberFormat.E164);
                log.info("Formatted number '{}' to E.164: {}", phoneNumberString, formattedNumber);
                return formattedNumber;
            } else {
                log.info("Phone number '{}' is invalid and cannot be formatted.", phoneNumberString);
            }
        } catch (NumberParseException e) {
            log.warn("Failed to parse phone number '{}' for formatting: {}", phoneNumberString, e.getMessage());
        }
        return null;
    }
}