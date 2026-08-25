package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.recommendation.entity.BandRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@IncludesPendingBands(reason = "추천 후보는 하이드레이션 단계(findAllByIdInAndStatus 등)에서 ACCEPTED로 걸러지고, 삭제 쿼리는 검수 밴드 정리용이다")
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

    // 검수 거절/더미 교체로 밴드 삭제 시 FK 정리 (band 도메인 RecommendationPort 어댑터에서 사용)
    // 호출측 트랜잭션의 영속성 컨텍스트를 비우면 안 되므로 clearAutomatically를 켜지 않는다
    @Modifying
    @Query("DELETE FROM BandRecommendationLog l WHERE l.band.id = :bandId")
    void deleteAllByBandId(@Param("bandId") Long bandId);
}
