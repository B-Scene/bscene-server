package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.request.SessionApplicationVisibilityRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationVisibilityResponse;
import com.umc.bscene.domain.session.dto.application.response.SessionApplicationSubmitResponse;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionApplicationCommandServiceImpl implements SessionApplicationCommandService {

    private final SessionApplicationRepository sessionApplicationRepository;
    private final SessionApplicationSubmissionRepository submissionRepository;
    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final BandMemberRepository bandMemberRepository;
    private final UserRepository userRepository;

    @Override
    public MySessionApplicationResponse createSessionApplication(
            Long userId,
            MySessionApplicationUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));

        SessionApplication sessionApplication = SessionApplication.builder()
                .userId(userId)
                .nickname(user.getName())
                .title(request.getTitle())
                .purpose(request.getPurpose())
                .profileImageUrl(request.getProfileImageUrl())
                .part(request.getPart())
                .skillLevel(request.getSkillLevel())
                .genre(request.getGenre())
                .region(request.getRegion())
                .intro(request.getIntro())
                .build();

        addPortfolioLinks(sessionApplication, request);

        SessionApplication savedSessionApplication = sessionApplicationRepository.save(sessionApplication);

        return MySessionApplicationResponse.from(savedSessionApplication);
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

        sessionApplication.updateApplication(
                request.getTitle(),
                request.getPurpose(),
                request.getProfileImageUrl(),
                request.getPart(),
                request.getSkillLevel(),
                request.getGenre(),
                request.getRegion(),
                request.getIntro()
        );

        sessionApplication.clearPortfolioLinks();
        addPortfolioLinks(sessionApplication, request);

        SessionApplication savedSessionApplication =
                sessionApplicationRepository.saveAndFlush(sessionApplication);

        return MySessionApplicationResponse.from(savedSessionApplication);
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

        bandMemberRepository
                .findBySessionApplication_SessionApplicationId(sessionApplicationId)
                .forEach(bandMember -> bandMember.clearSessionApplication());
        submissionRepository
                .deleteAllBySessionApplication_SessionApplicationId(sessionApplicationId);
        sessionApplicationRepository.delete(sessionApplication);
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
                .existsBySessionRecruitment_SessionRecruitmentIdAndSessionApplication_SessionApplicationId(
                        sessionRecruitmentId,
                        sessionApplicationId
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

        return SessionApplicationSubmitResponse.from(submission);
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
}
