package com.umc.bscene.domain.session.adapter;

import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.dto.response.session.SessionApplicationStatusResult;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.domain.user.port.SessionPort;
import com.umc.bscene.global.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class UserAdapter implements SessionPort {

    private final SessionApplicationSubmissionRepository sasRepository;
    private final SessionRecruitmentRepository srRepository;

    @Override
    public Long findBandIdBySessionApplicationSubmission(Long sasId) {

        Long bandId = sasRepository.findBandIdBySessionApplicationSubmissionId(sasId);

        if (bandId == null) {
            throw new SessionException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND);
        }

        return bandId;
    }

    @Override
    @Transactional
    public SessionApplicationStatusResult decideApplicationSubmission(Long sasId, Long deciderUserId, boolean isApproved) {

        SessionApplicationSubmission submission = sasRepository.findWithApplicationById(sasId)
                .orElseThrow(() -> new SessionException(SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND));

        SessionApplication application = submission.getSessionApplication();

        // 본인의 지원 건은 본인이 결정할 수 없음
        if (application.getUserId().equals(deciderUserId)) {
            throw new SessionException(SessionErrorCode.SELF_APPLICATION_DECISION_NOT_ALLOWED);
        }

        // 밴드 수락은 최종 확정이 아니라 지원자 확정 대기(BAND_ACCEPTED)로 전이
        // PENDING일 때만 원자적으로 전이 - 동시 결정, 지원자 취소와의 경합에서 한 건만 성공
        ApplicationStatus target = isApproved ? ApplicationStatus.BAND_ACCEPTED : ApplicationStatus.REJECTED;
        int updated = sasRepository.transitionStatus(sasId, ApplicationStatus.PENDING, target);
        if (updated == 0) {
            throw new SessionException(SessionErrorCode.APPLICATION_SUBMISSION_ALREADY_PROCESSED);
        }

        return toStatusResult(submission);
    }

    @Override
    @Transactional
    public SessionApplicationStatusResult finalizeApplicationSubmission(Long sasId, Long applicantUserId, boolean isAccepted) {

        // 본인의 지원 건이 아니면 존재 여부를 숨기기 위해 404
        SessionApplicationSubmission submission = sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(sasId, applicantUserId)
                .orElseThrow(() -> new SessionException(SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND));

        // 밴드가 수락한(BAND_ACCEPTED) 건만 지원자가 최종 확정 가능
        // 원자적 전이라 중복 확정 요청은 한 건만 성공
        ApplicationStatus target = isAccepted ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED;
        int updated = sasRepository.transitionStatus(sasId, ApplicationStatus.BAND_ACCEPTED, target);
        if (updated == 0) {
            throw new SessionException(SessionErrorCode.APPLICATION_SUBMISSION_NOT_CONFIRMABLE);
        }

        // 공고가 삭제되었으면 밴드를 특정할 수 없으므로 확정 불가 (전이 포함 전체 롤백)
        Long bandId = sasRepository.findBandIdBySessionApplicationSubmissionId(sasId);
        if (bandId == null) {
            throw new SessionException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND);
        }
        return toStatusResult(submission);
    }

    @Override
    public CursorPage<SessionRecruitmentResponse> findRecruitmentsByBandId(
            Long bandId, RecruitmentStatusFilter status, Long cursorId, int size) {

        LocalDateTime now = LocalDateTime.now();

        // hasNext 판별을 위해 한 건 더 조회
        Pageable limit = PageRequest.of(0, size + 1);

        // 1. 공고 단위 커서 페이지 (정렬·커서 조건은 쿼리가 결정)
        List<Long> recruitmentIds = (status == RecruitmentStatusFilter.OPEN)
                ? srRepository.findOpenRecruitmentIdsByBandId(bandId, now, cursorId, limit)
                : srRepository.findClosedRecruitmentIdsByBandId(bandId, now, cursorId, limit);

        boolean hasNext = recruitmentIds.size() > size;
        List<Long> pageIds = hasNext ? recruitmentIds.subList(0, size) : recruitmentIds;

        if (pageIds.isEmpty()) {
            return CursorPage.empty();
        }

        // 2. 페이지에 담긴 공고들의 지원자 플랫 행을 조회해 공고 단위로 그루핑
        List<BandsRecruitmentsSummaryResponse> rows = sasRepository.findApplicantsByRecruitmentIds(pageIds);
        List<SessionRecruitmentResponse> items = toSessionRecruitmentResponses(pageIds, rows);

        return hasNext
                ? CursorPage.ofHasNext(items, pageIds.get(pageIds.size() - 1))
                : CursorPage.ofLastPage(items);
    }

    // 공고 컬럼이 지원자 수만큼 중복된 플랫 행을, 페이지 쿼리가 정한 공고 순서대로 묶는다
    private List<SessionRecruitmentResponse> toSessionRecruitmentResponses(
            List<Long> orderedRecruitmentIds, List<BandsRecruitmentsSummaryResponse> rows) {

        Map<Long, List<BandsRecruitmentsSummaryResponse>> byRecruitment = new LinkedHashMap<>();
        for (BandsRecruitmentsSummaryResponse row : rows) {
            byRecruitment.computeIfAbsent(row.recruitmentId(), id -> new ArrayList<>()).add(row);
        }

        return orderedRecruitmentIds.stream()
                .map(byRecruitment::get)
                // 페이지 조회와 상세 조회 사이에 지원이 전부 취소되면 그룹이 빌 수 있음
                .filter(Objects::nonNull)
                .map(group -> {
                    BandsRecruitmentsSummaryResponse first = group.get(0);
                    List<SessionRecruitmentResponse.Recruiter> recruiters = group.stream()
                            .map(row -> new SessionRecruitmentResponse.Recruiter(
                                    row.sessionProfileId(),
                                    row.applySubmissionId(),
                                    row.applierProfileImageUrl(),
                                    row.applierNickname(),
                                    row.applierPart(),
                                    row.applierSkill(),
                                    row.applierRegion(),
                                    row.status()
                            ))
                            .toList();
                    return new SessionRecruitmentResponse(
                            first.recruitmentId(),
                            first.deadline(),
                            first.recruitPostTitle(),
                            first.recruitPart(),
                            first.recruitGenre(),
                            first.recruitRegion(),
                            recruiters
                    );
                })
                .toList();
    }

    private SessionApplicationStatusResult toStatusResult(
            SessionApplicationSubmission submission
    ) {
        return new SessionApplicationStatusResult(
                submission.getApplicationSubmissionId(),
                submission.getSessionRecruitment().getBand().getId(),
                submission.getSessionApplication().getUserId(),
                submission.getSessionApplication().getNickname(),
                submission.getSessionRecruitment().getRecruitmentTitle()
        );
    }
}
