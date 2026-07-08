package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BandProfileRepository extends JpaRepository<BandProfile, Long> {

    @Query("""
        SELECT DISTINCT bp
        FROM BandProfile bp
        LEFT JOIN FETCH bp.portfolioLinks
        WHERE bp.userId = :userId
          AND bp.deletedAt IS NULL
    """)
    Optional<BandProfile> findByUserIdWithPortfolioLinks(@Param("userId") Long userId);
}