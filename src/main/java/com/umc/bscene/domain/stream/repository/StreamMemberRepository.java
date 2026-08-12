package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamMemberRepository extends JpaRepository<StreamMember, Long> {

    List<StreamMember> findAllByAudioStream_Id(Long audioStreamId);

    // 라이브 홈 블럭별 coHost 매핑용: 여러 라이브의 확정(ACCEPTED) 진행자 행을 한 번에 조회
    List<StreamMember> findAllByAudioStream_IdInAndStatus(Collection<Long> audioStreamIds, StreamMemberStatus status);

    List<StreamMember> findAllByAudioStream_IdAndStatus(Long audioStreamId, StreamMemberStatus status);

    // 사용자 ID, 멤버 초대 상태, 방송 상태를 파라미터로 전달하면 레코드의 존재 여부 반환
    @Query("""
select count(sm) > 0
from StreamMember sm
where sm.user.id = :userId
    and sm.status = :memberStatus
    and sm.audioStream.status = :streamStatus
""")
    Boolean existsByIdWithStatuses(
            @Param("userId") Long userId,
            @Param("memberStatus") StreamMemberStatus memberStatus,
            @Param("streamStatus") StreamStatus streamStatus
    );

    // 현재 사용자의 공동 진행자 초대와 라이브 정보를 함께 조회
    @Query("""
    SELECT sm
    FROM StreamMember sm
    JOIN FETCH sm.audioStream
    WHERE sm.audioStream.id = :liveId
      AND sm.user.id = :userId
""")
    Optional<StreamMember> findWithStreamByLiveIdAndUserId(
            @Param("liveId") Long liveId,
            @Param("userId") Long userId
    );

    // MediaMTX read 인증용: 요청자가 해당 라이브의 확정(ACCEPTED) 진행자인지 확인
    boolean existsByAudioStream_IdAndUser_IdAndStatus(Long audioStreamId, Long userId, StreamMemberStatus status);

    // MediaMTX publish 인증용: 진행자 개인 송출 path로 멤버·소속 라이브를 한 번에 조회
    @Query("""
    SELECT sm
    FROM StreamMember sm
    JOIN FETCH sm.user
    JOIN FETCH sm.audioStream
    WHERE sm.path = :path
""")
    Optional<StreamMember> findWithUserAndStreamByPath(@Param("path") String path);

    // 진행자 프레젠스 갱신용(syncLiveState): 송출 중(ready)인 개인 path들의 멤버·유저·라이브를 폴 사이클당 한 번에 조회
    @Query("""
    SELECT sm
    FROM StreamMember sm
    JOIN FETCH sm.user
    JOIN FETCH sm.audioStream
    WHERE sm.path IN :paths
""")
    List<StreamMember> findAllWithUserAndStreamByPathIn(@Param("paths") Collection<String> paths);

    // 현재 상태가 expected일 때만 원자적으로 상태 변경
    @Modifying
    @Query("""
    UPDATE StreamMember sm
    SET sm.status = :target
    WHERE sm.id = :streamMemberId
      AND sm.status = :expected
""")
    int transitionStatus(
            @Param("streamMemberId") Long streamMemberId,
            @Param("expected") StreamMemberStatus expected,
            @Param("target") StreamMemberStatus target
    );
}
