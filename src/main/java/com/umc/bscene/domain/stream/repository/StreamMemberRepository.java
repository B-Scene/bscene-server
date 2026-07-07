package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.StreamMember;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StreamMemberRepository extends JpaRepository<StreamMember, Long> {

    @Query("""
select count(sm) > 0
from StreamMember sm
where sm.user.id = :userId
    and sm.status = :memberStatus
    and sm.audioStream.status = :streamStatus
""")
    Boolean existsByIdWithStatuses(Long userId, StreamStatus memberStatus, StreamStatus streamStatus);
}
