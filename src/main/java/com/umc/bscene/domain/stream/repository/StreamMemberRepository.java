package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamMemberStatus;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StreamMemberRepository extends JpaRepository<StreamMember, Long> {

    List<StreamMember> findAllByAudioStream_Id(Long audioStreamId);

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
}
