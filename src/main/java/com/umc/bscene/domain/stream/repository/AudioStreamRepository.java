package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // bandId로 현재 진행 중인 라이브 ID 조회
    Optional<AudioStream> findByBandIdAndStatus(Long bandId, StreamStatus status);

    // 경로 기입, status는 OPEN으로 고정하여 청취 여부 조회
    Boolean existsByPathAndStatus(String path, StreamStatus status);

    Boolean existsByBroadcasterIdAndStatus(Long userId, StreamStatus status);

    boolean existsByIdAndStatus(Long id, StreamStatus status);

    // 밴드의 라이브 이력 존재 여부 (검수 플로우 밴드 삭제 안전장치용)
    boolean existsByBandId(Long bandId);

    long countByBroadcasterIdAndStatus(Long broadcasterId, StreamStatus status);

    List<AudioStream> findAllByPathIn(List<String> paths);

    // 라이브 세션을 커서 페이지네이션
    @Query("""
select a from AudioStream as a
where a.path in :paths
    and (:cursor is null or a.id < :cursor)
    and a.status = 'OPEN'
    and a.bandId in (select b.id from Band b where b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED)
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
    and a.bandId in (select b.id from Band b where b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED)
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

    // 홈 예정 라이브 섹션용 상위 N개 (팬 모드: 전체, 검수 통과 밴드만)
    @Query("""
select a from AudioStream as a
where a.status = :status
    and a.scheduledAt > :now
    and a.bandId in (select b.id from Band b where b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED)
order by a.scheduledAt asc, a.id asc
""")
    List<AudioStream> findByStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
            @Param("status") StreamStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    // 홈 예정 라이브 섹션용 상위 N개 (밴드 모드: 내가 예약한 라이브만)
    List<AudioStream> findByBroadcasterIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
            Long broadcasterId,
            StreamStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    // 홈 예정 라이브 섹션용 상위 N개 (밴드 모드: 활성 밴드의 라이브만)
    List<AudioStream> findByBandIdAndStatusAndScheduledAtAfterOrderByScheduledAtAscIdAsc(
            Long bandId,
            StreamStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    // 홈 지금 라이브 중 섹션용 상위 N개 (밴드 모드: 활성 밴드의 라이브만, Redis 세션 대신 status = OPEN으로 진행 중 판정)
    List<AudioStream> findByBandIdAndStatusOrderByIdDesc(
            Long bandId,
            StreamStatus status,
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

    // 예약 편집: SCHEDULED 상태일 때만 원자적으로 갱신, null 필드는 coalesce로 기존 값 유지 (PATCH 시맨틱)
    // 조회-갱신 사이 enterRoom의 SCHEDULED -> OPEN 전환을 dirty-checking이 stale 값으로 덮는 lost update 방지
    @Modifying(clearAutomatically = true)
    @Query("""
update AudioStream a
set a.title = coalesce(:title, a.title),
    a.description = coalesce(:description, a.description),
    a.thumbnailImageUrl = coalesce(:thumbnailImageUrl, a.thumbnailImageUrl),
    a.scheduledAt = coalesce(:scheduledAt, a.scheduledAt)
where a.id = :id
    and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
""")
    int updateReservationIfScheduled(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
            @Param("thumbnailImageUrl") String thumbnailImageUrl,
            @Param("scheduledAt") LocalDateTime scheduledAt
    );

    // 예약 취소(soft-delete): SCHEDULED 상태일 때만 원자적으로 CANCELED 변경
    @Modifying(clearAutomatically = true)
    @Query("""
update AudioStream a
set a.status = com.umc.bscene.domain.stream.enums.StreamStatus.CANCELED,
    a.closedAt = :now
where a.id = :id
    and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
""")
    int cancelReservationIfScheduled(
            @Param("id") Long id,
            @Param("now") LocalDateTime now
    );

    // enterRoom: SCHEDULED -> OPEN 전환을 원자적으로 수행 (cancel의 SCHEDULED -> CANCELED와의 race condition 방지)
    @Modifying(clearAutomatically = true)
    @Query("""
update AudioStream a
set a.status = com.umc.bscene.domain.stream.enums.StreamStatus.OPEN,
    a.startedAt = case when a.startedAt is null then :now else a.startedAt end
where a.id = :id
    and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
""")
    int markStartedIfScheduled(
            @Param("id") Long id,
            @Param("now") LocalDateTime now
    );

    // 다시보기 등록 완료 알림 발송 권한을 한 작업자만 획득하도록 원자적으로 발송 시각 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update AudioStream a
    set a.replayNotificationSentAt = :sentAt
    where a.id = :id
        and a.replayNotificationSentAt is null
    """)
    int markReplayNotificationSentIfAbsent(
            @Param("id") Long id,
            @Param("sentAt") LocalDateTime sentAt
    );

    // 예약 편집/취소: coHost 읽기 전 행 선점으로 동시 PATCH 직렬화 및 cancel <-> PATCH insert race condition 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AudioStream a where a.id = :id")
    Optional<AudioStream> findByIdForUpdate(@Param("id") Long id);

    // 시작까지 30분 이내인 예정 라이브 중 밴드 구성원 알림을 발송하지 않은 라이브 조회
    @Query("""
    select a from AudioStream a
    where a.memberReminderSentAt is null
        and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
        and a.scheduledAt > :now
        and a.scheduledAt <= :reminderLimit
    order by a.scheduledAt asc, a.id asc
    """)
    List<AudioStream> findMemberReminderTargets(
            @Param("now") LocalDateTime now,
            @Param("reminderLimit") LocalDateTime reminderLimit
    );
}
