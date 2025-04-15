package com.axconstantino.reservationsystem.rooms.service;

import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.mapper.RoomMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class RoomService extends BaseCRUDService<Room, RoomDTO, Long, RoomRepository, RoomMapper> {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository repository, RoomMapper mapper) {
        super(repository, mapper);
        this.roomRepository = repository;
    }

    public Page<RoomDTO> getAvailableRooms(RoomFilterRequest filterRequest) {
        return roomRepository.findAvailableRooms(filterRequest);
    }
}
