package com.axconstantino.reservationsystem.booking.mapper;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.common.utils.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper extends BaseMapper<Booking, BookingResponse> {
    @Override
    @Mapping(source = "room.id", target = "roomId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "bookingStatus", target = "status")
    BookingResponse toDto(Booking booking);
}