package com.umc.bscene.domain.session.adapter;

import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.domain.user.port.SessionPort;
import com.umc.bscene.global.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
}
