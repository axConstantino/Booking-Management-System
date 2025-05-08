package com.axconstantino.reservationsystem.user.database.repository;

import com.axconstantino.reservationsystem.common.utils.BaseRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Finds all users with non-null reset tokens.
     * Optimized query to reduce in-memory processing load.
     *
     * @return List of users with active password reset tokens
     */
    @Query("SELECT u FROM User u WHERE u.resetToken IS NOT NULL")
    List<User> findAllWithResetToken();
}
