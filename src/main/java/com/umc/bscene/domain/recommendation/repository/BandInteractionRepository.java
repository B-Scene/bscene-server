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
