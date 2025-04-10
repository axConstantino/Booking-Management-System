package com.axconstantino.reservationsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhoneCustomValidator implements ConstraintValidator<ValidPhone, String> {
    private final PhoneValidator phoneValidator;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return phoneValidator.isValidPhoneNumber(value);
    }
}
