package com.arimar.gwent.ingame_service.repository;

import com.arimar.gwent.ingame_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
