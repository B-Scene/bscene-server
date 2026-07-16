package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.RecentRecruitmentListResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitmentView;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.SessionRecruitmentSortType;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentInterestRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentViewRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRecruitmentQueryServiceImpl implements SessionRecruitmentQueryService {

    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final SessionRecruitmentInterestRepository interestRepository;
    private final SessionRecruitmentViewRepository viewRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Override
    public SessionRecruitmentListResponse getSessionRecruitments(
            Long userId,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            SessionRecruitmentSortType sort,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : size;
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        LocalDateTime now = LocalDateTime.now();

        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();

        SessionRecruitmentSortType sortType = sort == null
                ? SessionRecruitmentSortType.LATEST
                : sort;
        List<SessionRecruitment> recruitments = switch (sortType) {
            case LATEST -> sessionRecruitmentRepository.findLatestRecruitments(
                    now, part, skillLevel, genre, region, normalizedKeyword, cursorId, pageRequest
            );
            case IMMINENT -> sessionRecruitmentRepository.findImminentRecruitments(
                    now, part, skillLevel, genre, region, normalizedKeyword, cursorId, pageRequest
            );
        };

        boolean hasNext = recruitments.size() > pageSize;

        List<SessionRecruitment> slicedRecruitments = hasNext
                ? recruitments.subList(0, pageSize)
                : recruitments;

        List<Long> recruitmentIds = slicedRecruitments.stream()
                .map(SessionRecruitment::getSessionRecruitmentId)
                .toList();
        Set<Long> interestedIds = recruitmentIds.isEmpty()
                ? Set.of()
                : interestRepository.findInterestedRecruitmentIds(userId, recruitmentIds);

        List<SessionRecruitmentListItemResponse> content = slicedRecruitments.stream()
                .map(recruitment -> toListItemResponse(recruitment, now, interestedIds))
                .toList();

        Long nextCursor = hasNext && !slicedRecruitments.isEmpty()
                ? slicedRecruitments.get(slicedRecruitments.size() - 1).getSessionRecruitmentId()
                : null;

        return SessionRecruitmentListResponse.builder()
                .content(content)
                .size(pageSize)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    // 세션 모집 공고 상세 조회
    @Override
    @Transactional
    public SessionRecruitmentDetailResponse getSessionRecruitmentDetail(
            Long userId,
            Long recruitmentId
    ) {

        SessionRecruitment recruitment = sessionRecruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(recruitmentId)
                .orElseThrow(() -> new SessionException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND));

        viewRepository
                .findBySessionRecruitment_SessionRecruitmentIdAndUser_Id(recruitmentId, userId)
                .ifPresent(viewRepository::delete);
        viewRepository.flush();
        viewRepository.save(SessionRecruitmentView.builder()
                .sessionRecruitment(recruitment)
                .user(userRepository.getReferenceById(userId))
                .build());

        return SessionRecruitmentDetailResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))

                // createdAt 기준 3일 이내면 true
                .isNew(isNewRecruitment(recruitment.getCreatedAt(), LocalDateTime.now()))
                .isInterested(interestRepository
                        .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                                recruitmentId,
                                userId
                        ))

                // 밴드 프로필 정보
                .bandId(recruitment.getBand().getId())
                .bandName(recruitment.getBand().getName())
                .bandProfileImageUrl(recruitment.getBand().getProfileImageUrl())
                .bandGenre(recruitment.getBand().getGenre().getName())
                .bandRegion(recruitment.getBand().getRegion().getName())


                // 모집 상세 정보
                .summary(recruitment.getSummary())
                .content(recruitment.getContent())
                .part(recruitment.getPart().getDescription())
                .genre(recruitment.getGenre().getDescription())
                .region(recruitment.getRegion())
                .practiceSchedule(recruitment.getPracticeSchedule())
                .practicePlace(recruitment.getPracticePlace())
                .qualification(recruitment.getQualification())
                .build();
    }

    private SessionRecruitmentListItemResponse toListItemResponse(
            SessionRecruitment recruitment,
            LocalDateTime now,
            Set<Long> interestedIds
    ) {
        return SessionRecruitmentListItemResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .bandId(recruitment.getBand().getId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .bandName(recruitment.getBand().getName())
                .bandGenre(recruitment.getBand().getGenre().getName())
                .bandRegion(recruitment.getBand().getRegion().getName())
                .summary(recruitment.getSummary())
                .part(recruitment.getPart())
                .skillLevel(recruitment.getSkillLevel())
                .practiceSchedule(recruitment.getPracticeSchedule())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))
                .isNew(isNewRecruitment(recruitment.getCreatedAt(), now))
                .isInterested(interestedIds.contains(recruitment.getSessionRecruitmentId()))
                .build();
    }

    @Override
    public RecentRecruitmentListResponse getRecentRecruitments(
            Long userId,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : Math.max(1, Math.min(size, 50));
        List<SessionRecruitmentView> views = viewRepository.findRecentViews(
                userId, cursorId, PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = views.size() > pageSize;
        List<SessionRecruitmentView> sliced = hasNext
                ? views.subList(0, pageSize)
                : views;
        List<Long> recruitmentIds = sliced.stream()
                .map(view -> view.getSessionRecruitment().getSessionRecruitmentId())
                .toList();
        Map<Long, SessionApplicationSubmission> submissions = recruitmentIds.isEmpty()
                ? Map.of()
                : submissionRepository
                        .findActiveSubmissionsForRecruitments(userId, recruitmentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                submission -> submission.getSessionRecruitment()
                                        .getSessionRecruitmentId(),
                                Function.identity(),
                                (latest, ignored) -> latest
                        ));
        List<RecentRecruitmentItemResponse> content = sliced.stream()
                .map(view -> {
                    Long recruitmentId = view.getSessionRecruitment()
                            .getSessionRecruitmentId();
                    SessionApplicationSubmission submission = submissions.get(recruitmentId);
                    return RecentRecruitmentItemResponse.of(
                            view,
                            submission == null ? null
                                    : submission.getSessionApplication().getTitle()
                    );
                })
                .toList();
        Long nextCursor = hasNext && !sliced.isEmpty()
                ? sliced.get(sliced.size() - 1).getSessionRecruitmentViewId()
                : null;
        return new RecentRecruitmentListResponse(
                content, pageSize, nextCursor, hasNext
        );
    }

    static boolean isNewRecruitment(LocalDateTime createdAt, LocalDateTime now) {
        return createdAt != null && now.isBefore(createdAt.plusDays(3));
    }

    private Long calculateDDay(LocalDate deadlineDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
    }
}
