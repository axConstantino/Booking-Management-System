package com.axconstantino.reservationsystem.user.dto;

import com.axconstantino.reservationsystem.constants.ValidationMessages;
import com.axconstantino.reservationsystem.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class UserDTO {
    @Size(min = 3, max = 50, message = ValidationMessages.USER_NAME_LENGTH)
    private String name;

    @Email(message = ValidationMessages.USER_EMAIL_INVALID)
    private String email;

    @ValidPhone(message = ValidationMessages.USER_PHONE_INVALID)
    private String phone;
}
