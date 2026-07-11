package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.dto.response.FanMyPageResponse;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.port.FollowPort;
import com.umc.bscene.domain.user.port.PerformancePort;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final FanProfileRepository fanProfileRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final FollowPort followPort;
    private final PerformancePort performancePort;

    // 팬모드 마이페이지 조회
    public FanMyPageResponse getFanMyPage(User user) {
        // 닉네임 : 팬 모드 사용자는 반드시 팬 프로필을 가짐 (없으면 데이터 이상)
        String nickname = fanProfileRepository.findByUser(user)
                .map(FanProfile::getNickname)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));

        List<Genre> genres = userGenresRepository.findAllByUser(user).stream()
                .map(UserGenres::getGenre)
                .toList();

        List<Region> regions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .toList();

        Long userId = user.getId();
        long followingCount = followPort.countFollowing(userId);
        long interestedPerformanceCount = performancePort.countInterested(userId);
        long participatedPerformanceCount = performancePort.countParticipated(userId);

        return FanMyPageResponse.of(
                nickname,
                genres,
                regions,
                user.getCurrentMode(),
                followingCount,
                interestedPerformanceCount,
                participatedPerformanceCount
        );
    }
}
