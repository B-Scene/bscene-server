package com.umc.bscene.domain.notification.repository;

import com.umc.bscene.domain.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    void deleteByUser_IdAndToken(Long userId, String token);

    List<PushToken> findAllByUser_Id(Long userId);
}