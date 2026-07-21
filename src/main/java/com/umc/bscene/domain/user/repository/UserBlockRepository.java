package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByAudioStream_IdAndBlocker_IdAndBlocked_Id(
            Long liveId, Long blockerId, Long blockedId);
    void deleteByAudioStream_IdAndBlocker_IdAndBlocked_Id(
            Long liveId, Long blockerId, Long blockedId);

    @Query("""
            select case when b.blocker.id = :userId then b.blocked.id else b.blocker.id end
            from UserBlock b
            where b.audioStream.id = :liveId
              and (b.blocker.id = :userId or b.blocked.id = :userId)
            """)
    Set<Long> findBlockedUserIdsRelatedTo(
            @Param("liveId") Long liveId, @Param("userId") Long userId);

    // 사용자가 차단한 모든 유저 id (라이브 구분 없이) — 게시물 댓글 목록에서 차단 유저 댓글 숨김용
    @Query("""
            select distinct b.blocked.id
            from UserBlock b
            where b.blocker.id = :userId
            """)
    Set<Long> findAllBlockedIdsByBlockerId(@Param("userId") Long userId);
}
