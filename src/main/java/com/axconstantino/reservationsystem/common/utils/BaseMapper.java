package com.axconstantino.reservationsystem.common.utils;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

public interface BaseMapper<E, D>{
    D toDto(E entity);

    E toEntity(D dto);

    void updateFromDTO(@MappingTarget E entity, D updateRequest);

    default List<D> toDtoList(List<E> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

}
