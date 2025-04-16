package com.axconstantino.reservationsystem.booking.database.repository;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends BaseRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b WHERE b.room = :room AND +" +
            "(b.startDate < :endDate AND b.endDate > :startDate)")
    List<Booking> findByRoomAndDatesOverlap(
            @Param("room") Room room,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}
