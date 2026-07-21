package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.LiveAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveAlarmRepository extends JpaRepository<LiveAlarm, Long> {

    Optional<LiveAlarm> findByAudioStream_IdAndUser_Id(Long audioStreamId, Long userId);

    // 예정된 라이브 목록 조회에서 사용자가 알림 설정한 라이브 ID만 추출
    @Query("""
select la.audioStream.id from LiveAlarm la
where la.user.id = :userId
    and la.audioStream.id in :liveIds
""")
    List<Long> findAlarmedLiveIds(
            @Param("userId") Long userId,
            @Param("liveIds") Collection<Long> liveIds
    );

    // 현재부터 30분 이내에 시작하는 예정 라이브 중 아직 리마인드를 발송하지 않은 알림 설정 조회
    @Query("""
    select la from LiveAlarm la
    join fetch la.audioStream a
    join fetch la.user u
    where la.reminderSentAt is null
        and a.status = com.umc.bscene.domain.stream.enums.StreamStatus.SCHEDULED
        and a.scheduledAt > :now
        and a.scheduledAt <= :reminderLimit
    order by a.scheduledAt asc, a.id asc
    """)
    List<LiveAlarm> findReminderTargets(
            @Param("now") LocalDateTime now,
            @Param("reminderLimit") LocalDateTime reminderLimit
    );
}
