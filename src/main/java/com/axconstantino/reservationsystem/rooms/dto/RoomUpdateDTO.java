package com.axconstantino.reservationsystem.rooms.dto;

import com.axconstantino.reservationsystem.constants.ValidationMessages;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomStatus;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RoomUpdateDTO {
    @Size(min = 3, max = 50, message = ValidationMessages.ROOM_NAME_LENGTH)
    private String name;

    @Positive(message = ValidationMessages.ROOM_PRICE_POSITIVE)
    @Digits(integer = 6, fraction = 2, message = "Price must have maximum 6 integer and 2 decimal digits")
    private Double pricePerNight;

    @Min(value = 1, message = ValidationMessages.CAPACITY_MIN)
    @Max(value = 10, message = ValidationMessages.CAPACITY_MAX)
    private Integer capacity;

    private RoomType type;

    @Size(max = 500, message = ValidationMessages.ROOM_DESC_LENGTH)
    private String description;

    @Pattern(regexp = "^[a-zA-Z0-9, ]+$", message = "Amenities can only contain letters, numbers and commas")
    private String amenities;

    private RoomStatus status;
}
