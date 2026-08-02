package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.SessionPushMessage;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationCreateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionApplicationCareer;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.event.SessionPortfolioPreviewRequestedEvent;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.port.BandMemberPort;
import com.umc.bscene.domain.session.port.NotifyPort;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionApplicationCommandServiceImpl implements SessionApplicationCommandService {

    private static final String DEFAULT_PURPOSE = "기본";

    private final SessionApplicationRepository sessionApplicationRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final SessionBasicProfileRepository sessionBasicProfileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final BandMemberPort bandMemberPort;
    private final NotifyPort notifyPort;

    @Override
    public MySessionApplicationResponse createSessionApplication(
            Long userId,
            MySessionApplicationCreateRequest request
    ) {
        validateDefaultApplicationCreation(userId, request.getPurpose());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));

        SessionApplication sessionApplication = SessionApplication.builder()
                .userId(userId)
                .nickname(user.getName())
                .title(request.getTitle())
                .purpose(request.getPurpose())
                .oneLineIntro(request.getOneLineIntro())
                .part(request.getPart())
                .skillLevel(request.getSkillLevel())
                .genre(request.getGenre())
                .region(request.getRegion())
                .intro(request.getIntro())
                .build();

        sessionApplication.updateVisibility(DEFAULT_PURPOSE.equals(request.getPurpose()));
        sessionApplication.replaceAvailableActivities(request.getAvailableActivities());
        addCareers(sessionApplication, request);
        addPortfolioLinks(sessionApplication, request);

        SessionApplication savedSessionApplication =
                sessionApplicationRepository.saveAndFlush(sessionApplication);
        publishPortfolioPreviewRequests(savedSessionApplication);

        return MySessionApplicationResponse.fromWithoutVisibility(
                savedSessionApplication,
                findSessionProfileImageUrl(userId)
        );
    }

    @Override
    public MySessionApplicationResponse updateSessionApplication(
            Long userId,
            Long sessionApplicationId,
            MySessionApplicationUpdateRequest request
    ) {
        SessionApplication sessionApplication = sessionApplicationRepository
                .findByIdAndUserIdWithPortfolioLinks(sessionApplicationId, userId)
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        validateDefaultApplicationUpdate(
                userId,
                sessionApplicationId,
                sessionApplication.getPurpose(),
                request.getPurpose()
        );

        sessionApplication.updateApplication(
                request.getTitle(),
                request.getPurpose(),
                request.getOneLineIntro(),
                request.getPart(),
                request.getSkillLevel(),
                request.getGenre(),
                request.getRegion(),
                request.getIntro()
        );
        sessionApplication.replaceAvailableActivities(request.getAvailableActivities());

        sessionApplication.clearPortfolioLinks();
        sessionApplication.clearCareers();
        addCareers(sessionApplication, request);
        addPortfolioLinks(sessionApplication, request);

        SessionApplication savedSessionApplication =
                sessionApplicationRepository.saveAndFlush(sessionApplication);
        publishPortfolioPreviewRequests(savedSessionApplication);

        return MySessionApplicationResponse.fromWithoutVisibility(
                savedSessionApplication,
                findSessionProfileImageUrl(userId)
        );
    }

    private String findSessionProfileImageUrl(Long userId) {
        return sessionBasicProfileRepository.findByUser_Id(userId)
                .map(profile -> profile.getProfileImageUrl())
                .orElse(null);
    }

    @Override
    public void deleteSessionApplication(Long userId, Long sessionApplicationId) {
        SessionApplication sessionApplication = sessionApplicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        sessionApplicationId,
                        userId
                )
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        sessionApplication.delete();
    }

    private void validateDefaultApplicationCreation(Long userId, String purpose) {
        if (sessionApplicationRepository.countByUserIdAndDeletedAtIsNull(userId) == 0
                && !DEFAULT_PURPOSE.equals(purpose)) {
            throw new SessionApplicationException(
                    SessionErrorCode.FIRST_SESSION_APPLICATION_MUST_BE_DEFAULT
            );
        }

        if (DEFAULT_PURPOSE.equals(purpose)
                && sessionApplicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNull(userId, DEFAULT_PURPOSE)) {
            throw new SessionApplicationException(
                    SessionErrorCode.DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    private void validateDefaultApplicationUpdate(
            Long userId,
            Long sessionApplicationId,
            String currentPurpose,
            String purpose
    ) {
        if (DEFAULT_PURPOSE.equals(currentPurpose) && !DEFAULT_PURPOSE.equals(purpose)) {
            throw new SessionApplicationException(
                    SessionErrorCode.DEFAULT_SESSION_APPLICATION_PURPOSE_IMMUTABLE
            );
        }

        if (DEFAULT_PURPOSE.equals(purpose)
                && sessionApplicationRepository
                .existsByUserIdAndPurposeAndDeletedAtIsNullAndSessionApplicationIdNot(
                        userId,
                        DEFAULT_PURPOSE,
                        sessionApplicationId
                )) {
            throw new SessionApplicationException(
                    SessionErrorCode.DEFAULT_SESSION_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    @Override
    public SessionApplicationVisibilityResponse updateVisibility(
            Long userId,
            Long sessionApplicationId,
            SessionApplicationVisibilityRequest request
    ) {
        SessionApplication sessionApplication = sessionApplicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        sessionApplicationId,
                        userId
                )
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        if (!"기본".equals(sessionApplication.getPurpose())) {
            throw new SessionApplicationException(
                    SessionErrorCode.SESSION_APPLICATION_VISIBILITY_NOT_ALLOWED
            );
        }

        sessionApplication.updateVisibility(request.getIsPublic());
        return new SessionApplicationVisibilityResponse(
                sessionApplicationId,
                sessionApplication.getIsPublic()
        );
    }

    @Override
    public SessionApplicationSubmitResponse submitApplication(
            Long userId,
            Long sessionRecruitmentId,
            Long sessionApplicationId
    ) {
        SessionRecruitment recruitment = sessionRecruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(sessionRecruitmentId)
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND
                ));

        if (!recruitment.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new SessionApplicationException(
                    SessionErrorCode.SESSION_RECRUITMENT_APPLICATION_CLOSED
            );
        }

        if (recruitment.getBand().getOwner().getId().equals(userId)) {
            throw new SessionApplicationException(
                    SessionErrorCode.SELF_RECRUITMENT_APPLICATION_NOT_ALLOWED
            );
        }

        SessionApplication application = sessionApplicationRepository
                .findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
                        sessionApplicationId,
                        userId
                )
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.SESSION_APPLICATION_NOT_FOUND
                ));

        if (submissionRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationIdAndStatusNot(
                        sessionRecruitmentId,
                        sessionApplicationId,
                        ApplicationStatus.CANCELED
                )) {
            throw new SessionApplicationException(
                    SessionErrorCode.SESSION_APPLICATION_ALREADY_SUBMITTED
            );
        }

        SessionApplicationSubmission submission = submissionRepository.save(
                SessionApplicationSubmission.builder()
                        .sessionRecruitment(recruitment)
                        .sessionApplication(application)
                        .status(ApplicationStatus.PENDING)
                        .build()
        );

        // 지원서 저장 후 밴드 구성원 전체에게 알림 발송
        notifyRecruitmentMembersAfterCommit(submission);

        return SessionApplicationSubmitResponse.from(submission);
    }

    @Override
    public void cancelSubmission(Long userId, Long applicationSubmissionId) {
        SessionApplicationSubmission submission = submissionRepository
                .findByApplicationSubmissionIdAndSessionApplication_UserId(
                        applicationSubmissionId,
                        userId
                )
                .orElseThrow(() -> new SessionApplicationException(
                        SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND
                ));

        if (submission.getStatus() != ApplicationStatus.PENDING) {
            throw new SessionApplicationException(
                    SessionErrorCode.APPLICATION_SUBMISSION_CANCEL_NOT_ALLOWED
            );
        }

        submission.cancel();
    }

    private void addPortfolioLinks(
            SessionApplication sessionApplication,
            MySessionApplicationUpdateRequest request
    ) {
        if (request.getPortfolioLinks() == null) {
            return;
        }

        request.getPortfolioLinks().stream()
                .filter(linkRequest -> linkRequest.getUrl() != null)
                .filter(linkRequest -> !linkRequest.getUrl().isBlank())
                .forEach(linkRequest ->
                        sessionApplication.addPortfolioLink(
                                SessionApplicationLink.builder()
                                        .sessionApplication(sessionApplication)
                                        .url(linkRequest.getUrl())
                                        .build()
                        )
                );
    }

    // 지원서 저장 트랜잭션 완료 후 밴드 구성원 전체에게 알림 발송
    private void notifyRecruitmentMembersAfterCommit(
            SessionApplicationSubmission submission
    ) {
        SessionRecruitment recruitment = submission.getSessionRecruitment();

        List<Long> receiverIds = bandMemberPort.getAcceptedMemberUserIds(
                recruitment.getBand().getId()
        );

        if (receiverIds.isEmpty()) {
            return;
        }

        SessionPushMessage message = SessionPushMessage.applicationSubmitted(
                submission.getApplicationSubmissionId(),
                submission.getSessionApplication().getNickname(),
                recruitment.getRecruitmentTitle()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receiverIds, message);
                    }
                }
        );
    }

    private void addCareers(
            SessionApplication sessionApplication,
            MySessionApplicationUpdateRequest request
    ) {
        if (request.getCareers() == null) {
            return;
        }
        request.getCareers().forEach(career ->
                sessionApplication.addCareer(SessionApplicationCareer.builder()
                        .sessionApplication(sessionApplication)
                        .name(career.getName())
                        .period(career.getPeriod())
                        .description(career.getDescription())
                        .build())
        );
    }

    private void addCareers(
            SessionApplication sessionApplication,
            MySessionApplicationCreateRequest request
    ) {
        if (request.getCareers() == null) {
            return;
        }
        request.getCareers().forEach(career ->
                sessionApplication.addCareer(SessionApplicationCareer.builder()
                        .sessionApplication(sessionApplication)
                        .name(career.getName())
                        .period(career.getPeriod())
                        .description(career.getDescription())
                        .build())
        );
    }

    private void addPortfolioLinks(
            SessionApplication sessionApplication,
            MySessionApplicationCreateRequest request
    ) {
        if (request.getPortfolioLinks() == null) {
            return;
        }
        request.getPortfolioLinks().forEach(link ->
                sessionApplication.addPortfolioLink(SessionApplicationLink.builder()
                        .sessionApplication(sessionApplication)
                        .url(link.getUrl())
                        .build())
        );
    }

    private void publishPortfolioPreviewRequests(SessionApplication application) {
        application.getPortfolioLinks().stream()
                .filter(link -> link.getDeletedAt() == null)
                .map(SessionApplicationLink::getSessionApplicationLinkId)
                .filter(java.util.Objects::nonNull)
                .map(SessionPortfolioPreviewRequestedEvent::new)
                .forEach(eventPublisher::publishEvent);
    }
}
