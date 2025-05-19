package com.axconstantino.reservationsystem.validation;

import com.axconstantino.reservationsystem.common.exception.InvalidPhoneNumberException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PhoneCustomValidator implements ConstraintValidator<ValidPhone, String> {

    private static PhoneValidator phoneValidator;

    @Autowired
    public PhoneCustomValidator(PhoneValidator phoneValidator) {
        PhoneCustomValidator.phoneValidator = phoneValidator;
    }

    public PhoneCustomValidator() {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            return phoneValidator.isValidPhoneNumber(value);
        } catch (InvalidPhoneNumberException e) {
            return false;
        }
    }
}
