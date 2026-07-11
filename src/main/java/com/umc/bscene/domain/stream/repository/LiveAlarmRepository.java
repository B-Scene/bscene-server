package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.LiveAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
