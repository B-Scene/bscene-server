package com.umc.bscene.domain.stream.repository;

import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
