package com.axconstantino.reservationsystem.booking.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

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
    private String startDate;
    private String endDate;
    private Double totalPrice;
    private String status; // e.g., "CONFIRMED", "CANCELLED"
}
