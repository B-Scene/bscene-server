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
import java.util.List;
import java.util.Optional;

public interface AudioStreamRepository extends JpaRepository<AudioStream, Long> {

    // Path 기준으로 쿼리.
    Optional<AudioStream> findByPath(String path);

    // 경로 기입, status는 OPEN으로 고정하여 청취 여부 조회
    Boolean existsByPathAndStatus(String path, StreamStatus status);

    Boolean existsByBroadcasterIdAndStatus(Long userId, StreamStatus status);

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
    a.scheduledAt = coalesce(:scheduledAt, a.scheduledAt)
where a.id = :id
    and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
""")
    int updateReservationIfScheduled(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
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

    // 예약 편집/취소: coHost 읽기 전 행 선점으로 동시 PATCH 직렬화 및 cancel <-> PATCH insert race condition 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AudioStream a where a.id = :id")
    Optional<AudioStream> findByIdForUpdate(@Param("id") Long id);
}
