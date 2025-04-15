package com.axconstantino.reservationsystem.rooms.mapper;

import com.axconstantino.reservationsystem.common.utils.BaseMapper;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper extends BaseMapper<Room, RoomDTO> {
}
