package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

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

    @Query("""
        SELECT submission
        FROM SessionApplicationSubmission submission
        JOIN FETCH submission.sessionRecruitment recruitment
        JOIN FETCH recruitment.band
        JOIN FETCH submission.sessionApplication application
        WHERE application.userId = :userId
          AND (:cursorId IS NULL OR submission.applicationSubmissionId < :cursorId)
        ORDER BY submission.applicationSubmissionId DESC
    """)
    List<SessionApplicationSubmission> findMySubmissions(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    Optional<SessionApplicationSubmission> findByApplicationSubmissionIdAndSessionApplication_UserId(
            Long applicationSubmissionId,
            Long userId
    );

    @Query("""
        SELECT DISTINCT submission
        FROM SessionApplicationSubmission submission
        JOIN FETCH submission.sessionApplication application
        LEFT JOIN FETCH application.portfolioLinks
        WHERE submission.applicationSubmissionId = :submissionId
          AND application.userId = :userId
    """)
    Optional<SessionApplicationSubmission> findMySubmissionWithApplication(
            @Param("submissionId") Long submissionId,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT DISTINCT submission
        FROM SessionApplicationSubmission submission
        JOIN FETCH submission.sessionRecruitment recruitment
        JOIN FETCH recruitment.band band
        JOIN FETCH submission.sessionApplication application
        LEFT JOIN FETCH application.portfolioLinks
        WHERE submission.applicationSubmissionId = :submissionId
          AND band.owner.id = :ownerId
    """)
    Optional<SessionApplicationSubmission> findForRecruitmentOwner(
            @Param("submissionId") Long submissionId,
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT submission
        FROM SessionApplicationSubmission submission
        JOIN FETCH submission.sessionRecruitment recruitment
        JOIN FETCH submission.sessionApplication application
        WHERE application.userId = :userId
          AND recruitment.sessionRecruitmentId IN :recruitmentIds
          AND submission.status <> com.umc.bscene.domain.session.enums.ApplicationStatus.CANCELED
        ORDER BY submission.applicationSubmissionId DESC
    """)
    List<SessionApplicationSubmission> findActiveSubmissionsForRecruitments(
            @Param("userId") Long userId,
            @Param("recruitmentIds") Collection<Long> recruitmentIds
    );
}
