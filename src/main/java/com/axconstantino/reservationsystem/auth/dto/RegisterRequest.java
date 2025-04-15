package com.axconstantino.reservationsystem.auth.dto;

import com.axconstantino.reservationsystem.constants.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = ValidationMessages.REGISTER_NAME_REQUIRED)
    @Size(min = 3, max = 50, message = ValidationMessages.REGISTER_NAME_SIZE)
    private String name;

    @Email(message = ValidationMessages.REGISTER_EMAIL_INVALID)
    @NotBlank(message = ValidationMessages.REGISTER_EMAIL_REQUIRED)
    private String email;

    @NotBlank(message = ValidationMessages.REGISTER_PASSWORD_REQUIRED)
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = ValidationMessages.REGISTER_PASSWORD_PATTERN)
    private String password;
}
