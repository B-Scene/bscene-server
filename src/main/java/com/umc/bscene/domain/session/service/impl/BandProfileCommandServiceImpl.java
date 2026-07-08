package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.profile.request.MyBandProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MyBandProfileResponse;
import com.umc.bscene.domain.session.entity.BandProfile;
import com.umc.bscene.domain.session.entity.BandProfileLink;
import com.umc.bscene.domain.session.repository.BandProfileRepository;
import com.umc.bscene.domain.session.service.BandProfileCommandService;
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
public class BandProfileCommandServiceImpl implements BandProfileCommandService {

    private final BandProfileRepository sessionProfileRepository;
    private final UserRepository userRepository;

    @Override
    public MyBandProfileResponse saveMySessionProfile(
            Long userId,
            MyBandProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));

        BandProfile sessionProfile = sessionProfileRepository.findByUserIdWithPortfolioLinks(userId)
                .orElseGet(() -> BandProfile.builder()
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
                                    BandProfileLink.builder()
                                            .sessionProfile(sessionProfile)
                                            .url(linkRequest.getUrl())
                                            .build()
                            )
                    );
        }

        BandProfile savedSessionProfile = sessionProfileRepository.save(sessionProfile);

        return MyBandProfileResponse.from(savedSessionProfile);
    }
}