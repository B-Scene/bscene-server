package com.umc.bscene.domain.auth.repository;

import com.umc.bscene.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    long deleteByExpiresAtBefore(LocalDateTime now);
}