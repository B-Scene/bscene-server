package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // 커서 페이지에 담긴 공고들의 지원서·지원자 정보를 플랫 행으로 조회 (공고 단위 그루핑은 어댑터에서)
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
where sr.sessionRecruitmentId in :recruitmentIds
    and sas.status <> CANCELED
order by sas.applicationSubmissionId asc
""")
    List<BandsRecruitmentsSummaryResponse> findApplicantsByRecruitmentIds(
            @Param("recruitmentIds") Collection<Long> recruitmentIds
    );

    // 세션 지원 기록 ID로 모집 공고를 게시한 밴드 ID를 조회 (삭제된 공고의 지원 건은 제외)
    @Query("""
select sas.sessionRecruitment.band.id
from SessionApplicationSubmission sas
where sas.applicationSubmissionId = :sasId
    and sas.sessionRecruitment.deletedAt is null
""")
    Long findBandIdBySessionApplicationSubmissionId(
            @Param("sasId") Long sasId);

    // 상태 전이를 원자적으로 수행 - 현재 상태가 expected일 때만 갱신됨
    // 반환값 0이면 다른 요청이 먼저 전이시킨 것 (동시 결정·확정·취소 경합 방지)
    @Modifying
    @Query("""
        UPDATE SessionApplicationSubmission submission
        SET submission.status = :target
        WHERE submission.applicationSubmissionId = :submissionId
          AND submission.status = :expected
    """)
    int transitionStatus(
            @Param("submissionId") Long submissionId,
            @Param("expected") ApplicationStatus expected,
            @Param("target") ApplicationStatus target
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

    // 지원 수락/거절 처리용 - 지원자(userId) 확인을 위해 지원서까지 한 번에 조회
    @Query("""
        SELECT submission
        FROM SessionApplicationSubmission submission
        JOIN FETCH submission.sessionApplication
        WHERE submission.applicationSubmissionId = :submissionId
    """)
    Optional<SessionApplicationSubmission> findWithApplicationById(
            @Param("submissionId") Long submissionId
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

    Optional<SessionApplicationSubmission>
    findFirstBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_UserIdOrderByApplicationSubmissionIdDesc(
            Long recruitmentId, Long userId);
}
