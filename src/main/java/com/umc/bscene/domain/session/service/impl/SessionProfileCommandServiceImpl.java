package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.profile.request.MySessionProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MySessionProfileResponse;
import com.umc.bscene.domain.session.entity.SessionProfile;
import com.umc.bscene.domain.session.entity.SessionProfileLink;
import com.umc.bscene.domain.session.repository.SessionProfileRepository;
import com.umc.bscene.domain.session.service.SessionProfileCommandService;
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
public class SessionProfileCommandServiceImpl implements SessionProfileCommandService {

    private final SessionProfileRepository sessionProfileRepository;
    private final UserRepository userRepository;

    @Override
    public MySessionProfileResponse saveMySessionProfile(
            Long userId,
            MySessionProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));

        SessionProfile sessionProfile = sessionProfileRepository.findByUserIdWithPortfolioLinks(userId)
                .orElseGet(() -> SessionProfile.builder()
                        .userId(userId)
                        .nickname(user.getName())
                        .part(request.getPart())
                        .skillLevel(request.getSkillLevel())
                        .genre(request.getGenre())
                        .region(request.getRegion())
                        .intro(request.getIntro())
                        .build());

        sessionProfile.updateProfile(
                request.getPart(),
                request.getSkillLevel(),
                request.getGenre(),
                request.getRegion(),
                request.getIntro()
        );

        sessionProfile.clearPortfolioLinks();

        if (request.getPortfolioLinks() != null) {
            request.getPortfolioLinks().stream()
                    .filter(linkRequest -> linkRequest.getUrl() != null)
                    .filter(linkRequest -> !linkRequest.getUrl().isBlank())
                    .forEach(linkRequest ->
                            sessionProfile.addPortfolioLink(
                                    SessionProfileLink.builder()
                                            .sessionProfile(sessionProfile)
                                            .url(linkRequest.getUrl())
                                            .build()
                            )
                    );
        }

        SessionProfile savedSessionProfile = sessionProfileRepository.save(sessionProfile);

        return MySessionProfileResponse.from(savedSessionProfile);
    }
}