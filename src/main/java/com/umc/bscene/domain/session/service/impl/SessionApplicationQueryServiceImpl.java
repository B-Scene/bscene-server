package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchItemResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSearchResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationDetailResponse;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationSummaryResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionItemResponse;
import com.umc.bscene.domain.session.dto.application.response.MyApplicationSubmissionListResponse;
import com.umc.bscene.domain.session.dto.application.response.SubmittedApplicationDetailResponse;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import com.umc.bscene.domain.session.service.SessionApplicationQueryService;
import com.umc.bscene.domain.session.service.SessionRecruitmentSearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionApplicationQueryServiceImpl implements SessionApplicationQueryService {

    private static final String DEFAULT_PURPOSE = "기본";

    private final SessionApplicationRepository sessionApplicationRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final SessionBasicProfileRepository sessionBasicProfileRepository;
    private final UserRepository userRepository;
    private final SessionRecruitmentSearchKeywordService searchKeywordService;

    @Override
    public MySessionApplicationDetailResponse getMySessionApplicationDetail(
            Long userId,
            Long sessionApplicationId
    ) {
        SessionApplication application = sessionApplicationRepository
                .findByIdAndUserIdWithPortfolioLinks(sessionApplicationId, userId)
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        return MySessionApplicationDetailResponse.from(application);
    }

    @Override
    public MySessionApplicationSummaryResponse getMySessionApplicationSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));
        SessionBasicProfile sessionProfile = sessionBasicProfileRepository
                .findByUser_Id(userId)
                .orElse(null);
        SessionApplication defaultApplication = sessionApplicationRepository
                .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                        userId,
                        DEFAULT_PURPOSE
                )
                .orElse(null);

        long applicationCount = sessionApplicationRepository
                .countByUserIdAndDeletedAtIsNull(userId);
        long submissionCount = submissionRepository
                .countBySessionApplication_UserId(userId);
        long inProgressCount = submissionRepository
                .countBySessionApplication_UserIdAndStatus(
                        userId,
                        ApplicationStatus.PENDING
                );
        List<SessionApplication> applications = sessionApplicationRepository
                .findAllByUserIdAndDeletedAtIsNullOrderBySessionApplicationIdAsc(userId);

        return MySessionApplicationSummaryResponse.of(
                defaultApplication,
                user.getName(),
                sessionProfile == null ? null : sessionProfile.getProfileImageUrl(),
                applicationCount,
                submissionCount,
                inProgressCount,
                applications
        );
    }

    @Override
    public MyApplicationSubmissionListResponse getMyApplicationSubmissions(
            Long userId,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : Math.max(1, Math.min(size, 50));
        List<SessionApplicationSubmission> submissions = submissionRepository
                .findMySubmissions(
                        userId,
                        cursorId,
                        PageRequest.of(0, pageSize + 1)
                );

        boolean hasNext = submissions.size() > pageSize;
        List<SessionApplicationSubmission> slicedSubmissions = hasNext
                ? submissions.subList(0, pageSize)
                : submissions;
        List<MyApplicationSubmissionItemResponse> content = slicedSubmissions.stream()
                .map(MyApplicationSubmissionItemResponse::from)
                .toList();
        Long nextCursor = hasNext && !slicedSubmissions.isEmpty()
                ? slicedSubmissions.get(slicedSubmissions.size() - 1)
                        .getApplicationSubmissionId()
                : null;

        return new MyApplicationSubmissionListResponse(
                content,
                pageSize,
                nextCursor,
                hasNext
        );
    }

    @Override
    @Transactional
    public SubmittedApplicationDetailResponse getSubmittedApplication(
            Long viewerId,
            Long applicationSubmissionId
    ) {
        SessionApplicationSubmission submission = submissionRepository
                .findForRecruitmentMember(applicationSubmissionId, viewerId)
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND
                ));
        submission.markChecked();

        SessionApplication application = submission.getSessionApplication();
        SessionBasicProfile profile = sessionBasicProfileRepository
                .findByUser_Id(application.getUserId())
                .orElse(null);
        String userName = userRepository.findById(application.getUserId())
                .map(User::getName)
                .orElse(application.getNickname());
        SessionApplicationDetailResponse detail = SessionApplicationDetailResponse.from(
                application,
                userName,
                profile == null ? null : profile.getProfileImageUrl()
        );
        return SubmittedApplicationDetailResponse.of(submission, detail);
    }

    @Override
    public SessionApplicationDetailResponse getMySubmittedApplication(
            Long userId,
            Long applicationSubmissionId
    ) {
        SessionApplicationSubmission submission = submissionRepository
                .findMySubmissionWithApplication(applicationSubmissionId, userId)
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND
                ));
        SessionApplication application = submission.getSessionApplication();
        SessionBasicProfile profile = sessionBasicProfileRepository
                .findByUser_Id(userId)
                .orElse(null);
        String userName = userRepository.findById(userId)
                .map(User::getName)
                .orElse(application.getNickname());

        return SessionApplicationDetailResponse.from(
                application,
                userName,
                profile == null ? null : profile.getProfileImageUrl()
        );
    }

    @Override
    public SessionApplicationDetailResponse getDefaultApplicationDetail(
            Long sessionApplicationId
    ) {
        SessionApplication application = sessionApplicationRepository
                .findPublicDetailWithPortfolioLinks(
                        sessionApplicationId,
                        DEFAULT_PURPOSE
                )
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        SessionBasicProfile sessionProfile = sessionBasicProfileRepository
                .findByUser_Id(application.getUserId())
                .orElse(null);
        String userName = userRepository.findById(application.getUserId())
                .map(User::getName)
                .orElse(application.getNickname());
        return SessionApplicationDetailResponse.from(
                application,
                userName,
                sessionProfile == null ? null : sessionProfile.getProfileImageUrl()
        );
    }

    @Override
    @Transactional
    public SessionApplicationSearchResponse searchDefaultApplications(
            Long viewerUserId,
            Region region,
            SkillLevel skillLevel,
            Part part,
            Genre genre,
            String keyword,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : Math.max(1, Math.min(size, 50));
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
        searchKeywordService.record(viewerUserId, normalizedKeyword);

        boolean explicitCondition = region != null
                || skillLevel != null
                || part != null
                || genre != null
                || normalizedKeyword != null;
        SessionApplication myDefaultApplication = explicitCondition
                ? null
                : sessionApplicationRepository
                        .findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
                                viewerUserId,
                                DEFAULT_PURPOSE
                        )
                        .orElse(null);
        boolean recommendationEnabled = myDefaultApplication != null
                && sessionApplicationRepository.existsRecommendedApplications(
                        viewerUserId,
                        DEFAULT_PURPOSE,
                        myDefaultApplication.getGenre(),
                        myDefaultApplication.getRegion()
                );

        List<SessionApplication> applications = sessionApplicationRepository
                .searchDefaultApplications(
                        viewerUserId,
                        DEFAULT_PURPOSE,
                        region,
                        skillLevel,
                        part,
                        genre,
                        recommendationEnabled,
                        recommendationEnabled ? myDefaultApplication.getGenre() : null,
                        recommendationEnabled ? myDefaultApplication.getRegion() : null,
                        normalizedKeyword,
                        cursorId,
                        PageRequest.of(0, pageSize + 1)
                );

        boolean hasNext = applications.size() > pageSize;
        List<SessionApplication> slicedApplications = hasNext
                ? applications.subList(0, pageSize)
                : applications;

        List<Long> userIds = slicedApplications.stream()
                .map(SessionApplication::getUserId)
                .distinct()
                .toList();
        Map<Long, SessionBasicProfile> sessionProfiles = userIds.isEmpty()
                ? Map.of()
                : sessionBasicProfileRepository.findAllByUser_IdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                profile -> profile.getUser().getId(),
                                Function.identity()
                        ));
        Map<Long, String> userNames = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getName));

        List<SessionApplicationSearchItemResponse> content = slicedApplications.stream()
                .map(application -> {
                    SessionBasicProfile profile = sessionProfiles.get(application.getUserId());
                    return SessionApplicationSearchItemResponse.from(
                            application,
                            userNames.get(application.getUserId()),
                            profile == null ? null : profile.getProfileImageUrl()
                    );
                })
                .toList();
        Long nextCursor = hasNext && !slicedApplications.isEmpty()
                ? slicedApplications.get(slicedApplications.size() - 1)
                        .getSessionApplicationId()
                : null;

        return SessionApplicationSearchResponse.builder()
                .content(content)
                .size(pageSize)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
