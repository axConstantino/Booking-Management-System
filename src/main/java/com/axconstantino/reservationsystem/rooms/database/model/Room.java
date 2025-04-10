package com.axconstantino.reservationsystem.rooms.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "rooms")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Room {
    private UUID id;
    private String name;
    private Double pricePerNight;
    private String description;
    private String amenities;
    private Boolean isAvailable;
}
