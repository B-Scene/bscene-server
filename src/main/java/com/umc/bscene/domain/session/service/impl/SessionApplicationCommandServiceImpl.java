package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionApplicationException;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.service.SessionApplicationCommandService;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionApplicationCommandServiceImpl implements SessionApplicationCommandService {

    private final SessionApplicationRepository sessionApplicationRepository;
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
