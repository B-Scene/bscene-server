package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.service.SessionApplicationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionApplicationQueryServiceImpl implements SessionApplicationQueryService {

    private final SessionApplicationRepository sessionApplicationRepository;

    @Override
    public List<MySessionApplicationResponse> getMySessionApplications(Long userId) {
        return sessionApplicationRepository.findAllByUserIdWithPortfolioLinks(userId).stream()
                .map(MySessionApplicationResponse::from)
                .toList();
    }
}
