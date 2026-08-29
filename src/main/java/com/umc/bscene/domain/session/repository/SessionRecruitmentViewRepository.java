package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.session.entity.SessionRecruitmentView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionRecruitmentViewRepository extends JpaRepository<SessionRecruitmentView, Long> {

    Optional<SessionRecruitmentView> findBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
            Long recruitmentId,
            Long userId
    );

    @Query("""
        SELECT view
        FROM SessionRecruitmentView view
        JOIN FETCH view.sessionRecruitment recruitment
        JOIN FETCH recruitment.band
        WHERE view.user.id = :userId
          AND recruitment.deletedAt IS NULL
          AND (:cursorId IS NULL OR view.sessionRecruitmentViewId < :cursorId)
        ORDER BY view.sessionRecruitmentViewId DESC
    """)
    @IncludesPendingBands(reason = "세션 모집은 활동 생성 게이트(BAND_NOT_VERIFIED)로 ACCEPTED 밴드에서만 생성된다")
    List<SessionRecruitmentView> findRecentViews(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
