package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import org.springframework.data.domain.Page;

public interface RoomRepositoryCustom {
    Page<RoomDTO> findAvailableRooms(RoomFilterRequest filter);
}
