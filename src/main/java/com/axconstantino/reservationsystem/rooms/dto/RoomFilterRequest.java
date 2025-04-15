package com.axconstantino.reservationsystem.rooms.dto;

import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomFilterRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer capacity;
    private Double minPrice;
    private Double maxPrice;
    private RoomType type;
    private String sortBy = "price";
    private String order = "asc";
    private Integer page = 0;
    private Integer size = 10;
}
