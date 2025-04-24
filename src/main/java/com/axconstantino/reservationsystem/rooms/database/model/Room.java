package com.axconstantino.reservationsystem.rooms.database.model;

import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomStatus;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal pricePerNight;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomType type;

    @Column(nullable = false)
    @Size(max = 500)
    private String description;

    @Column(nullable = false)
    private String amenities;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomStatus status;
}
