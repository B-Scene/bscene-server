package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.band.dto.request.MyBandProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.MyBandProfileResponse;
import com.umc.bscene.domain.band.entity.BandProfile;
import com.umc.bscene.domain.band.entity.BandProfileLink;
import com.umc.bscene.domain.band.repository.BandProfileRepository;
import com.umc.bscene.domain.session.service.BandProfileCommandService;
import com.umc.bscene.global.exception.BaseException;
import com.umc.bscene.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BandProfileCommandServiceImpl implements BandProfileCommandService {

    private final BandProfileRepository bandProfileRepository;

    @Override
    public MyBandProfileResponse saveMySessionProfile(
            Long userId,
            MyBandProfileUpdateRequest request
    ) {
        BandProfile bandProfile = bandProfileRepository.findByUserIdWithPortfolioLinks(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.UNAUTHORIZED_ERROR));

        bandProfile.updateProfile(
                request.getNickname(),
                request.getPart(),
                request.getSkillLevel(),
                request.getGenre(),
                request.getRegion(),
                request.getIntro()
        );

        bandProfile.clearPortfolioLinks();

        if (request.getPortfolioLinks() != null) {
            request.getPortfolioLinks().stream()
                    .filter(linkRequest -> linkRequest.getUrl() != null)
                    .filter(linkRequest -> !linkRequest.getUrl().isBlank())
                    .forEach(linkRequest ->
                            bandProfile.addPortfolioLink(
                                    BandProfileLink.builder()
                                            .bandProfile(bandProfile)
                                            .url(linkRequest.getUrl())
                                            .build()
                            )
                    );
        }

        BandProfile savedBandProfile = bandProfileRepository.save(bandProfile);

        return MyBandProfileResponse.from(savedBandProfile);
    }
}