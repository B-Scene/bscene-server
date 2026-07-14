package com.umc.bscene.domain.user.repository;

import com.umc.bscene.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    @Query("select count(b) > 0 from UserBlock b where "
            + "(b.blocker.id = :firstUserId and b.blocked.id = :secondUserId) or "
            + "(b.blocker.id = :secondUserId and b.blocked.id = :firstUserId)")
    boolean existsBetween(Long firstUserId, Long secondUserId);
}
