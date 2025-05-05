package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import org.springframework.data.domain.Page;

/**
 * Custom repository interface for advanced queries on Room entities.
 */
public interface RoomRepositoryCustom {
    /**
     * Finds available rooms based on filter criteria.
     *
     * @param filter the filtering criteria including capacity, type, price, date range, etc.
     * @return a page of available RoomDTOs
     */
    Page<RoomDTO> findAvailableRooms(RoomFilterRequest filter);
}
