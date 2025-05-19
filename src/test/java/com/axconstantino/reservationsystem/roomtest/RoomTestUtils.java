package com.axconstantino.reservationsystem.roomtest;

import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomStatus;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomType;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;

import java.time.LocalDate;

public class RoomTestUtils {

    public static RoomDTO createTestRoomDTO(Long id) {
        return RoomDTO.builder()
                .name("Deluxe Suite")
                .capacity(4)
                .pricePerNight(250.0)
                .type(RoomType.DELUXE)
                .status(RoomStatus.AVAILABLE)
                .build();
    }

    public static RoomFilterRequest createValidFilterRequest() {
        return RoomFilterRequest.builder()
                .page(0)
                .size(10)
                .sortBy("pricePerNight")
                .order("asc")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .capacity(2)
                .minPrice(100.0)
                .maxPrice(300.0)
                .type(RoomType.STANDARD)
                .build();
    }

    public static RoomFilterRequest createInvalidFilterRequest() {
        return RoomFilterRequest.builder()
                .page(-1)
                .size(150)
                .sortBy("invalidField")
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now())
                .build();
    }
}