package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface SessionApplicationSubmissionRepository
        extends JpaRepository<SessionApplicationSubmission, Long> {

    // 활성화된 공고에 지원한 지원자 수를 세는 쿼리
    @Query("""
select count(submission)
from SessionApplicationSubmission submission
join submission.sessionRecruitment recruitment
where recruitment.band.id = :bandId
    and recruitment.deletedAt IS NULL
    and recruitment.deadlineAt > :now
    and submission.status <> com.umc.bscene.domain.session.enums.ApplicationStatus.CANCELED
""")
    long countActiveApplicantsByBandId(
            @Param("bandId") Long bandId,
            @Param("now") LocalDateTime now
    );

    // bandId로 활성화된 공고 조회, 반환은 지원서와 지원자 정보
    // 마감하지 않은, 마감일 임박순 정렬
    @Query("""
select 
    new com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse(
            sr.sessionRecruitmentId,
            sas.applicationSubmissionId,
            sbp.sessionBasicProfileId,
            sr.deadlineAt,
            sr.recruitmentTitle,
            sr.part,
            sr.genre,
            sr.region,
            sa.profileImageUrl,
            sa.nickname,
            sa.part,
            sa.skillLevel,
            sa.region,
            sas.status
    )
from 
SessionApplicationSubmission sas
join sas.sessionApplication sa
join sas.sessionRecruitment sr
join SessionBasicProfile sbp
    on sbp.user.id = sa.userId
where sr.band.id = :bandId
    and sr.deadlineAt > :now
    and sr.deletedAt is null
    and sas.status <> CANCELED
order by sr.deadlineAt asc
""")
    List<BandsRecruitmentsSummaryResponse> findNonExpiredApplicantsByBandIdOrderByAsc(
            @Param("bandId") Long bandId,
            @Param("now") LocalDateTime now
    );

    // bandId로 마감된 공고 조회, 반환은 지원서와 지원자 정보
    // 마감된지 얼마 안된 것부터 마감된지 오래된 것 순
    @Query("""
select 
    new com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse(
            sr.sessionRecruitmentId,
            sas.applicationSubmissionId,
            sbp.sessionBasicProfileId,
            sr.deadlineAt,
            sr.recruitmentTitle,
            sr.part,
            sr.genre,
            sr.region,
            sa.profileImageUrl,
            sa.nickname,
            sa.part,
            sa.skillLevel,
            sa.region,
            sas.status
    )
from 
SessionApplicationSubmission sas
join sas.sessionApplication sa
join sas.sessionRecruitment sr
join SessionBasicProfile sbp
    on sbp.user.id = sa.userId
where sr.band.id = :bandId
    and sr.deadlineAt <= :now
    and sr.deletedAt is null
    and sas.status <> CANCELED
order by sr.deadlineAt desc
""")
    List<BandsRecruitmentsSummaryResponse> findExpiredApplicantsByBandIdOrderByDesc(
            @Param("bandId") Long bandId,
            @Param("now") LocalDateTime now
    );

    long countBySessionApplication_UserId(Long userId);

    long countBySessionApplication_UserIdAndStatus(
            Long userId,
            ApplicationStatus status
    );

    void deleteAllBySessionApplication_SessionApplicationId(Long sessionApplicationId);

    boolean existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationIdAndStatusNot(
            Long sessionRecruitmentId,
            Long sessionApplicationId,
            ApplicationStatus status
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
          AND (
              band.owner.id = :viewerId
              OR EXISTS (
                  SELECT bm.id
                  FROM BandMember bm
                  WHERE bm.band = band
                    AND bm.user.id = :viewerId
                    AND bm.status = com.umc.bscene.domain.band.enums.BandMemberStatus.ACCEPTED
              )
          )
    """)
    Optional<SessionApplicationSubmission> findForRecruitmentMember(
            @Param("submissionId") Long submissionId,
            @Param("viewerId") Long viewerId
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

    Optional<SessionApplicationSubmission>
    findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
            Long recruitmentId, Long userId);
}
