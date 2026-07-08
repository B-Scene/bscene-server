package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.profile.response.MyBandProfileResponse;
import com.umc.bscene.domain.session.repository.BandProfileRepository;
import com.umc.bscene.domain.session.service.BandProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandProfileQueryServiceImpl implements BandProfileQueryService {

    private final BandProfileRepository sessionProfileRepository;

    @Override
    public MyBandProfileResponse getMySessionProfile(Long userId) {
        return sessionProfileRepository.findByUserIdWithPortfolioLinks(userId)
                .map(MyBandProfileResponse::from)
                .orElseGet(() -> MyBandProfileResponse.empty(userId));
    }
}