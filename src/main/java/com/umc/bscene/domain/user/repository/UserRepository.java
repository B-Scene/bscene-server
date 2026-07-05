package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPhone(String phone);

    List<User> findByNameContaining(String keyword);
}
