package com.axconstantino.reservationsystem.user.database.repository;

import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
