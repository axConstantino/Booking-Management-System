package com.axconstantino.reservationsystem.rooms.service;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.dto.RoomUpdateDTO;
import com.axconstantino.reservationsystem.rooms.mapper.RoomMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class that manages room-related operations.
 * <p>
 *     This service extends generic CRUD service to provide base functionality,
 *     and includes custom business logic for handling room availability and updates.
 *</p>
 *
 * Responsibilities:
 * <ul>
 *     <li>Fetch available rooms based on filter criteria.</li>
 *     <li>Update room details.</li>
 * </ul>
 *
 */
@Slf4j
@Service
public class RoomService extends BaseCRUDService<Room, RoomDTO, Long, RoomRepository, RoomMapper> {

    /**
     * Constructs the RoomService with required dependencies.
     *
     * @param repository the RoomRepository to access room data
     * @param mapper     the RoomMapper for converting between entities and DTOs
     */
    public RoomService(RoomRepository repository, RoomMapper mapper) {
        super(repository, mapper);
    }

    /**
     * Retrieves a paginated list of available rooms based on filter criteria.
     *
     * @param filterRequest the filtering criteria such as date range, capacity, location, etc.
     * @return a page of RoomDTOs representing the available rooms
     */
    @Transactional(readOnly = true)
    public Page<RoomDTO> getAvailableRooms(RoomFilterRequest filterRequest) {
        log.info("Fetching available rooms with filters: {}", filterRequest);
        Page<RoomDTO> availableRooms = repository.findAvailableRooms(filterRequest);
        log.info("Found {} available rooms", availableRooms.getTotalElements());
        return availableRooms;
    }

    /**
     * Updates the information of a specific room.
     *
     * @param roomId        the ID of the room to update
     * @param updateRequest the new room data to apply
     * @return the updated RoomDTO
     * @throws NotFoundException if the room with the specified ID does not exist
     */
    @Transactional
    public RoomDTO updateRoom(Long roomId, RoomUpdateDTO updateRequest) {
        log.info("Attempting to update room with ID: {}", roomId);

        Room room = repository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("Room not found with ID: {}", roomId);
                    return new NotFoundException("Room not found with id: " + roomId);
                });

        log.debug("Original room data: {}", room);

        mapper.updateEntityFromDto(updateRequest, room);

        Room updatedRoom = repository.save(room);
        RoomDTO updatedDTO = mapper.toDto(updatedRoom);

        log.info("Room with ID: {} updated successfully", roomId);
        log.debug("Updated room data: {}", updatedDTO);

        return updatedDTO;
    }
}
