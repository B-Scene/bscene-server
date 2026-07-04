package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FanProfileRepository extends JpaRepository<FanProfile, Long> {

    Optional<FanProfile> findByUser(User user);

    boolean existsByNickname(String nickname);
}