package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.recommendation.entity.BandRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BandRecommendationLogRepository extends JpaRepository<BandRecommendationLog, Long> {

    // 노출 피로도 감쇠 계산용 : 후보 밴드별 노출 시각 원본 ([bandId, createdAt] 행).
    // position <= :maxVisiblePosition으로 실제 눈에 띄었을 상위 노출만 집계 대상으로 삼는다
    @Query("SELECT l.band.id, l.createdAt FROM BandRecommendationLog l " +
            "WHERE l.user.id = :userId AND l.band.id IN :bandIds " +
            "AND l.position <= :maxVisiblePosition AND l.createdAt >= :since")
    List<Object[]> findRecentVisibleExposures(
            @Param("userId") Long userId,
            @Param("bandIds") List<Long> bandIds,
            @Param("maxVisiblePosition") int maxVisiblePosition,
            @Param("since") LocalDateTime since
    );
}
