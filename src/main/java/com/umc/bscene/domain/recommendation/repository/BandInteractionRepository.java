package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.recommendation.entity.BandInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BandInteractionRepository extends JpaRepository<BandInteraction, Long> {

    // 유저의 클릭 수 상위 N개 밴드 조회 (limit은 Pageable로 전달)
    List<BandInteraction> findByUser_IdOrderByClickCountDesc(Long userId, Pageable pageable);

    // 노출 피로도 리셋 시점 판단용 : 후보 밴드 중 클릭(관심) 이력이 있는 밴드의 마지막 클릭 시각
    @Query("SELECT bi.band.id, bi.lastInteractedAt FROM BandInteraction bi " +
            "WHERE bi.user.id = :userId AND bi.band.id IN :bandIds")
    List<Object[]> findLastInteractedAtByUserIdAndBandIdIn(
            @Param("userId") Long userId,
            @Param("bandIds") List<Long> bandIds
    );

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO band_interaction (user_id, band_id, click_count, last_interacted_at, created_at, updated_at) " +
            "VALUES (:userId, :bandId, 1, :interactedAt, :now, :now) " +
            "ON DUPLICATE KEY UPDATE " +
            "click_count = click_count + IF(last_interacted_at <= :dedupBefore, 1, 0), " +
            "last_interacted_at = IF(last_interacted_at <= :dedupBefore, :interactedAt, last_interacted_at), " +
            "updated_at = :now",
            nativeQuery = true)
    void upsertInteraction(@Param("userId") Long userId,
                            @Param("bandId") Long bandId,
                            @Param("interactedAt") LocalDateTime interactedAt,
                            @Param("dedupBefore") LocalDateTime dedupBefore,
                            @Param("now") LocalDateTime now);
}
