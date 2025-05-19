package com.axconstantino.reservationsystem.rooms.dto;

import com.axconstantino.reservationsystem.constants.ValidationMessages;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomStatus;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoomDTO {
    @NotBlank(message = ValidationMessages.ROOM_NAME_REQUIRED)
    @Size(min = 3, max = 50, message = ValidationMessages.ROOM_NAME_LENGTH)
    private String name;

    @Positive(message = ValidationMessages.ROOM_PRICE_POSITIVE)
    @Digits(integer = 6, fraction = 2, message = "Price must have maximum 6 integer and 2 decimal digits")
    private Double pricePerNight;

    @Min(value = 1, message = ValidationMessages.ROOM_CAPACITY_MIN)
    @Max(value = 10, message = ValidationMessages.ROOM_CAPACITY_MAX)
    private Integer capacity;

    @NotNull(message = ValidationMessages.ROOM_TYPE_REQUIRED)
    private RoomType type;

    @Size(max = 500, message = ValidationMessages.ROOM_DESC_LENGTH)
    private String description;

    @NotBlank(message = ValidationMessages.ROOM_AMENITIES_REQUIRED)
    @Pattern(regexp = "^[a-zA-Z0-9, ]+$", message = "Amenities can only contain letters, numbers and commas")
    private String amenities;

    @NotNull(message = ValidationMessages.ROOM_STATUS_REQUIRED)
    private RoomStatus status;

    private List<RoomImageDTO> images;
}
