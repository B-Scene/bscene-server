package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserAvailableModes;
import com.umc.bscene.domain.user.enums.UserMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAvailableModesRepository extends JpaRepository<UserAvailableModes, Long> {

    List<UserAvailableModes> findAllByUser(User user);

    boolean existsByUserAndMode(User user, UserMode mode);
}