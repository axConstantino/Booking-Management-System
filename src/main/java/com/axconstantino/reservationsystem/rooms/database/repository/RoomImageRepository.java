package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.rooms.database.model.RoomImage;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomImageRepository extends BaseRepository<RoomImage, Long> {
}
