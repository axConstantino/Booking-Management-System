package com.axconstantino.reservationsystem.user.mapper;

import com.axconstantino.reservationsystem.common.utils.BaseMapper;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserDTO, UserBasicInfoUpdateDto> {

    @Override
    UserDTO toDto(User entity);

    @Override
    User toEntity(UserDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UserBasicInfoUpdateDto updateRequestDTO, User entity);

}
