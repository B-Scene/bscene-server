package com.umc.bscene.domain.session.adapter;

import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.dto.response.session.ReceiveRecruitmentsResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.port.SessionPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class UserAdapter implements SessionPort {

    private final SessionApplicationSubmissionRepository sasRepository;
    private final SessionBasicProfileRepository sbpRepository;

    @Override
    public ReceiveRecruitmentsResponse findPendingRecruitmentsByBandId(Long userId, Long bandId) {

        LocalDateTime now = LocalDateTime.now();

        // 현재 진행 중인 공고 조회
        List<BandsRecruitmentsSummaryResponse> nonExpired = sasRepository.findNonExpiredApplicantsByBandIdOrderByAsc(bandId, now);

        // 이미 종료된 공고 조회
        List<BandsRecruitmentsSummaryResponse> expired = sasRepository.findExpiredApplicantsByBandIdOrderByDesc(bandId, now);

        // 중복 행 그루핑
        return new ReceiveRecruitmentsResponse(
                toSessionRecruitmentResponses(nonExpired),
                toSessionRecruitmentResponses(expired)
        );
    }

    // 공고 컬럼이 지원자 수만큼 중복된 플랫 행을 공고 단위로 묶는다 (쿼리 정렬 순서 유지)
    private List<SessionRecruitmentResponse> toSessionRecruitmentResponses(List<BandsRecruitmentsSummaryResponse> rows) {
        Map<Long, List<BandsRecruitmentsSummaryResponse>> byRecruitment = new LinkedHashMap<>();
        for (BandsRecruitmentsSummaryResponse row : rows) {
            byRecruitment.computeIfAbsent(row.recruitmentId(), id -> new ArrayList<>()).add(row);
        }

        return byRecruitment.values().stream()
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

}
