package com.axconstantino.reservationsystem.common.utils;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface BaseRepository<E, ID extends Serializable> extends JpaRepository<E, ID> {

    default E getExisted(ID id, Class<?> entityClass) {
        return findById(id)
                .orElseThrow(() -> new NotFoundException(entityClass, id));

    }
}
