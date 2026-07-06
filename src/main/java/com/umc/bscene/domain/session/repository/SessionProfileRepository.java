package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SessionProfileRepository extends JpaRepository<SessionProfile, Long> {

    @Query("""
        SELECT DISTINCT sp
        FROM SessionProfile sp
        LEFT JOIN FETCH sp.portfolioLinks
        WHERE sp.userId = :userId
        AND sp.deletedAt IS NULL
    """)
    Optional<SessionProfile> findByUserIdWithPortfolioLinks(@Param("userId") Long userId);
}