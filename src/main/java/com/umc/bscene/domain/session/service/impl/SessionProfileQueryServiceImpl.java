package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.profile.response.MySessionProfileResponse;
import com.umc.bscene.domain.session.repository.SessionProfileRepository;
import com.umc.bscene.domain.session.service.SessionProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionProfileQueryServiceImpl implements SessionProfileQueryService {

    private final SessionProfileRepository sessionProfileRepository;

    @Override
    public MySessionProfileResponse getMySessionProfile(Long userId) {
        return sessionProfileRepository.findByUserIdWithPortfolioLinks(userId)
                .map(MySessionProfileResponse::from)
                .orElseGet(() -> MySessionProfileResponse.empty(userId));
    }
}