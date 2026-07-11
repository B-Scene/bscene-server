package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionApplicationSubmissionRepository
        extends JpaRepository<SessionApplicationSubmission, Long> {

    long countBySessionApplication_UserId(Long userId);

    long countBySessionApplication_UserIdAndStatus(
            Long userId,
            ApplicationStatus status
    );

    void deleteAllBySessionApplication_SessionApplicationId(Long sessionApplicationId);

    boolean existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationId(
            Long sessionRecruitmentId,
            Long sessionApplicationId
    );
}
