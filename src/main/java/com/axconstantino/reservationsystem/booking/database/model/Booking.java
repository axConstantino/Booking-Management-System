package com.axconstantino.reservationsystem.booking.database.model;

import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class Booking {
    private Long id;
    private Long roomId;
    private Long userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double totalPrice;
    private BookingStatus bookingStatus;
}
