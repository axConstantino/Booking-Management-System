package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends BaseRepository<Room, Long>, RoomRepositoryCustom {

}
