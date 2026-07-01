package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
