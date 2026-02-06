package com.arimar.gwent.authservice.repository;

import com.arimar.gwent.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserEntityRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByGameIdAndTag(String gameId, String tag);

    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByGameIdAndTag(String gameId, String tag);
}


