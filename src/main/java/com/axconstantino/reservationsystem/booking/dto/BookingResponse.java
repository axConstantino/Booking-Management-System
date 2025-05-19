package com.axconstantino.reservationsystem.booking.dto;

import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingResponse {
    private UUID id;
    private Long roomId;
    private UUID userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalPrice;
    private BookingStatus status;
}
