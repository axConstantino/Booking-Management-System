package com.axconstantino.reservationsystem.booking.dto;

import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private Long roomId;
    private UUID userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double totalPrice;
    private BookingStatus status;
}
