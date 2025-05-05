package com.axconstantino.reservationsystem.common.utils;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Optional;

/**
 * Generic base class form implementing CRUD operations.
 * This class encapsulates the common logic that can be reused across different services,
 * minimizing boilerplate code and promoting consistency.
 *
 * @param <E> Entity type.
 * @param <D> Data Transfer Object (DTO) type.
 * @param <ID> Type of the entity identifier (must be {@link Serializable}).
 * @param <R> Type of the repository, extending {@link BaseRepository}.
 * @param <M> Type of the mapper, extending {@link BaseMapper}.
 */
@RequiredArgsConstructor
public class BaseCRUDService<E, D, ID extends Serializable, R extends BaseRepository<E, ID>, M extends BaseMapper<E, D>> {
    /**
     * Repository used for persistence operations.
     */
    protected final R repository;

    /**
     * Mapper used to convert between entity and DTO.
     */
    protected final M mapper;

    /**
     * Retrieves an entity by its ID and maps it to a DTO.
     *
     * @param id the identifier of the entity
     * @return an {@link Optional} containing the DTO if found, or empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<D> get(ID id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }

    /**
     * Retrieves a paginated list of all entities, mapped to DTOs.
     *
     * @param pageable the pagination and sorting information
     * @return a {@link Page} of DTOs
     */
    @Transactional(readOnly = true)
    public Page<D> getAll(Pageable pageable) {
        Page<E> entityPage = repository.findAll(pageable);
        return entityPage.map(mapper::toDto);
    }

    /**
     * Creates a new entity from the given DTO and persists it.
     *
     * @param dto the DTO to convert and save
     * @return the saved entity mapped back to a DTO
     */
    @Transactional
    public D create(D dto) {
        E entity = mapper.toEntity(dto);
        E createdEntity = repository.save(entity);
        return mapper.toDto(createdEntity);
    }

    /**
     * Deletes an entity by its ID.
     * Throws a {@link NotFoundException} if the entity does not exist.
     *
     * @param id the identifier of the entity to delete
     * @throws NotFoundException if no entity with the given ID is found
     */
    @Transactional
    public void delete(ID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(id);
        }
        repository.deleteById(id);
    }
}

