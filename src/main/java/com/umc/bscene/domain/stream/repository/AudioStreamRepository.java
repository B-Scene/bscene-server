package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AudioStreamRepository extends JpaRepository<AudioStream, Long> {

    // Path 기준으로 쿼리.
    Optional<AudioStream> findByPath(String path);

    // 경로 기입, status는 OPEN으로 고정하여 청취 여부 조회
    Boolean existsByPathAndStatus(String path, StreamStatus status);

    Boolean existsByBroadcasterIdAndStatus(Long userId, StreamStatus status);

    long countByBroadcasterIdAndStatus(Long broadcasterId, StreamStatus status);

    List<AudioStream> findAllByPathIn(List<String> paths);

    // 라이브 세션을 커서 페이지네이션
    @Query("""
select a from AudioStream as a
where a.path in :paths
    and (:cursor is null or a.id < :cursor)
    and a.status = 'OPEN'
order by a.id desc
""")
    List<AudioStream> findLivePage(
            @Param("paths") List<String> paths,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // 팔로우한 밴드의 라이브 세션만 커서 페이지네이션 (BandMember 서브쿼리로 bandId → broadcasterId 변환 제거)
    @Query("""
select a from AudioStream as a
where a.path in :paths
    and a.bandId in :bandIds
    and (:cursor is null or a.id < :cursor)
    and a.status = 'OPEN'
order by a.id desc
""")
    List<AudioStream> findLivePageByBandIds(
            @Param("paths") List<String> paths,
            @Param("bandIds") Collection<Long> bandIds,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // 예정된 라이브를 시작 시각 오름차순으로 커서 페이지네이션 (동일 시각 충돌 방지를 위해 id를 보조 키로 사용)
    @Query("""
select a from AudioStream as a
where a.status = 'SCHEDULED'
    and a.scheduledAt > :now
    and (:cursorScheduledAt is null
        or a.scheduledAt > :cursorScheduledAt
        or (a.scheduledAt = :cursorScheduledAt and a.id > :cursorId))
order by a.scheduledAt asc, a.id asc
""")
    List<AudioStream> findUpcomingPage(
            @Param("now") LocalDateTime now,
            @Param("cursorScheduledAt") LocalDateTime cursorScheduledAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 팔로우한 밴드의 예정 라이브만 커서 페이지네이션 (BandMember 서브쿼리로 bandId → broadcasterId 변환 제거)
    @Query("""
select a from AudioStream as a
where a.status = 'SCHEDULED'
    and a.scheduledAt > :now
    and a.bandId in :bandIds
    and (:cursorScheduledAt is null
        or a.scheduledAt > :cursorScheduledAt
        or (a.scheduledAt = :cursorScheduledAt and a.id > :cursorId))
order by a.scheduledAt asc, a.id asc
""")
    List<AudioStream> findUpcomingPageByBandIds(
            @Param("now") LocalDateTime now,
            @Param("cursorScheduledAt") LocalDateTime cursorScheduledAt,
            @Param("cursorId") Long cursorId,
            @Param("bandIds") Collection<Long> bandIds,
            Pageable pageable
    );

    // 홈 예정 라이브 섹션용 상위 N개 (팬 모드: 전체)
    List<AudioStream> findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
            StreamStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    // 홈 예정 라이브 섹션용 상위 N개 (밴드 모드: 내가 예약한 라이브만)
    List<AudioStream> findByBroadcasterIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
            Long broadcasterId,
            StreamStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    Boolean existsByPath(String path);

    // status = SCHEDULED 조건을 DB에서 원자적으로 검사하는 조건부 벌크 UPDATE (읽기-수정 사이의 lost update 방지)
    // 방치 판정: 즉시 시작 방은 createdAt, 예약 방은 scheduledAt 기준(coalesce)
    @Modifying(clearAutomatically = true)
    @Query("""
update AudioStream a
set a.status = com.umc.bscene.domain.stream.enums.StreamStatus.CANCELED,
    a.closedAt = :now
where a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
    and coalesce(a.scheduledAt, a.createdAt) < :threshold
""")
    int cancelAbandonedScheduled(
            @Param("threshold") LocalDateTime threshold,
            @Param("now") LocalDateTime now
    );

    List<AudioStream> findByStatusAndStartedAtBefore(StreamStatus status, LocalDateTime thresholdAt);
}
