package com.axconstantino.reservationsystem.common.utils;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BaseCRUDService<E, D, ID extends Serializable, R extends BaseRepository<E, ID>, M extends BaseMapper<E, D>> {
    protected final R repository;
    protected final M mapper;

    @Transactional(readOnly = true)
    public Optional<D> get(ID id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<D> getAll(Pageable pageable) {
        Page<E> entityPage = repository.findAll(pageable);
        return entityPage.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<D> getAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Transactional
    public D create(D dto) {
        E entity = mapper.toEntity(dto);
        E createdEntity = repository.save(entity);
        return mapper.toDto(createdEntity);
    }

    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(id);
        }
        repository.deleteById(id);
    }
}

