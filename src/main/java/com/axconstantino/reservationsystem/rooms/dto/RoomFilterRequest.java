package com.axconstantino.reservationsystem.rooms.dto;

import com.axconstantino.reservationsystem.constants.ValidationMessages;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomFilterRequest {

    @NotNull(message = ValidationMessages.DATE_START_REQUIRED)
    @FutureOrPresent(message = ValidationMessages.DATE_START_FUTURE)
    private LocalDateTime startDate;

    @NotNull(message = ValidationMessages.DATE_END_REQUIRED)
    @Future(message = ValidationMessages.DATE_END_FUTURE)
    private LocalDateTime endDate;

    @Min(value = 1, message = ValidationMessages.CAPACITY_MIN)
    @Max(value = 10, message = ValidationMessages.CAPACITY_MAX)
    private Integer capacity;

    @PositiveOrZero(message = ValidationMessages.PRICE_NON_NEGATIVE)
    private Double minPrice;

    @PositiveOrZero(message = ValidationMessages.PRICE_NON_NEGATIVE)
    private Double maxPrice;

    private RoomType type;

    @NotBlank(message = ValidationMessages.SORT_FIELD_REQUIRED)
    @Pattern(regexp = "pricePerNight|capacity|type",
            message = ValidationMessages.SORT_FIELD_INVALID)
    private String sortBy = "pricePerNight";

    @NotBlank(message = ValidationMessages.ORDER_DIRECTION_REQUIRED)
    @Pattern(regexp = "asc|desc",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = ValidationMessages.ORDER_DIRECTION_INVALID)
    private String order = "asc";

    @Min(value = 0, message = ValidationMessages.PAGE_NON_NEGATIVE)
    private Integer page = 0;

    @Min(value = 1, message = ValidationMessages.SIZE_MIN)
    @Max(value = 100, message = ValidationMessages.SIZE_MAX)
    private Integer size = 10;

    @AssertTrue(message = ValidationMessages.DATE_RANGE_INVALID)
    public boolean isEndDateAfterStartDate() {
        return endDate == null || startDate == null || endDate.isAfter(startDate);
    }

    @AssertTrue(message = ValidationMessages.PRICE_RANGE_INVALID)
    public boolean isMaxPriceValid() {
        return minPrice == null || maxPrice == null || maxPrice >= minPrice;
    }
}