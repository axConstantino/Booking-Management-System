package com.axconstantino.reservationsystem.common.utils;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public class BaseCRUDService<E, D, ID extends Serializable, R extends BaseRepository<E, ID>, M extends BaseMapper<E, D>> {
    protected final R repository;
    protected final M mapper;

    @Transactional(readOnly = true)
    public D get(ID id, Class<?> entityClass) {
        E entity = repository.getExisted(id, entityClass);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<D> getAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Transactional
    public D create(D dto, Class<?> entityClass) {
        E entity = mapper.toEntity(dto);
        E createdEntity = repository.save(entity);
        return mapper.toDto(createdEntity);
    }

    @Transactional
    public void delete(ID id, Class<?> entityClass) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(entityClass, id);
        }
        repository.deleteById(id);
    }
}
