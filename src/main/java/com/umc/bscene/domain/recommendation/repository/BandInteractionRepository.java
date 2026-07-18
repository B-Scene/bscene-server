package com.umc.bscene.domain.recommendation.repository;

import com.umc.bscene.domain.recommendation.entity.BandInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BandInteractionRepository extends JpaRepository<BandInteraction, Long> {

    // 이미 (userId, bandId) row가 있는 경우: SELECT 없이 clickCount를 원자적으로 +1.
    // 대상 row가 없으면 0건 갱신되므로, row 존재 여부를 모르는 최초 상호작용 시점에는 upsertInteraction()을 사용할 것.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BandInteraction bi " +
            "SET bi.clickCount = bi.clickCount + 1, bi.lastInteractedAt = :interactedAt " +
            "WHERE bi.user.id = :userId AND bi.band.id = :bandId")
    int incrementClickCount(@Param("userId") Long userId,
                             @Param("bandId") Long bandId,
                             @Param("interactedAt") LocalDateTime interactedAt);

    // row 존재 여부를 모르는 최초 상호작용 시점에 사용. (userId, bandId) 유니크 제약을 이용한
    // MySQL INSERT ... ON DUPLICATE KEY UPDATE로 삽입/증가를 한 쿼리로 원자 처리해 동시성 문제를 없앤다.
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO BandInteraction (userId, bandId, clickCount, lastInteractedAt, createdAt, updatedAt) " +
            "VALUES (:userId, :bandId, 1, :interactedAt, :now, :now) " +
            "ON DUPLICATE KEY UPDATE " +
            "clickCount = clickCount + 1, lastInteractedAt = :interactedAt, updatedAt = :now",
            nativeQuery = true)
    void upsertInteraction(@Param("userId") Long userId,
                            @Param("bandId") Long bandId,
                            @Param("interactedAt") LocalDateTime interactedAt,
                            @Param("now") LocalDateTime now);
}
