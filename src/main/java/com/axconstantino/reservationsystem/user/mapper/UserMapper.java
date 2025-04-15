package com.axconstantino.reservationsystem.user.mapper;

import com.axconstantino.reservationsystem.common.utils.BaseMapper;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserDTO> {

    @Override
    UserDTO toDto(User entity);

    @Override
    User toEntity(UserDTO dto);

}
