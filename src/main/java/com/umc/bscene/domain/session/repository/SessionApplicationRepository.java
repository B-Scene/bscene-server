package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionApplicationRepository extends JpaRepository<SessionApplication, Long> {

    @Query("""
        SELECT DISTINCT sa
        FROM SessionApplication sa
        LEFT JOIN FETCH sa.portfolioLinks
        WHERE sa.userId = :userId
          AND sa.deletedAt IS NULL
        ORDER BY sa.sessionApplicationId ASC
    """)
    List<SessionApplication> findAllByUserIdWithPortfolioLinks(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT sa
        FROM SessionApplication sa
        LEFT JOIN FETCH sa.portfolioLinks
        WHERE sa.sessionApplicationId = :sessionApplicationId
          AND sa.userId = :userId
          AND sa.deletedAt IS NULL
    """)
    Optional<SessionApplication> findByIdAndUserIdWithPortfolioLinks(
            @Param("sessionApplicationId") Long sessionApplicationId,
            @Param("userId") Long userId
    );
}
