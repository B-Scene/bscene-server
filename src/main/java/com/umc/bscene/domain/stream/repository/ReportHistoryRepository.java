package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    @Query("""
            select distinct r.reporterId
            from ReportHistory r
            where r.audioStream.id = :liveId and r.targetUser.id = :targetUserId
            """)
    Set<Long> findReporterIdsByLiveIdAndTargetUserId(
            @Param("liveId") Long liveId,
            @Param("targetUserId") Long targetUserId
    );

    // 동일 라이브에서 동일 신고자가 동일 대상을 이미 신고했는지 (중복 신고 방어)
    boolean existsByAudioStream_IdAndTargetUser_IdAndReporterId(
            Long liveId,
            Long targetUserId,
            Long reporterId
    );

    // 디스코드 알림 미발송 건 스캔
    List<ReportHistory> findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
            LocalDateTime start,
            LocalDateTime end
    );

    // 벌크 UPDATE로 영속성 컨텍스트 없이도 마킹 가능
    @Transactional
    @Modifying
    @Query("update ReportHistory r set r.discordNotifiedAt = :notifiedAt where r.id = :id")
    void markDiscordNotified(@Param("id") Long id, @Param("notifiedAt") LocalDateTime notifiedAt);
}
