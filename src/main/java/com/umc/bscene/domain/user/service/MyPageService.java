package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.dto.response.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int MAX_PAGE_SIZE = 30;   // 목록 조회(참여 기록·관심 공연) 페이지 크기 상한

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

    // 공연 참여 기록 조회 (참여 완료 공연, 연도 필터, offset 무한스크롤)
    // 필터는 서버 기준 올해에 상대적 : THIS_YEAR(올해) / LAST_YEAR(작년) / BEFORE(재작년 이전) / ALL(전체)
    public ParticipationHistoryResponse getParticipationHistory(
            Long userId, HistoryYearFilter filter, int page, int size) {
        HistoryYearFilter appliedFilter = (filter == null) ? HistoryYearFilter.ALL : filter;
        int baseYear = LocalDate.now().getYear();

        LocalDate startDate = null;
        LocalDate endDate = null;
        switch (appliedFilter) {
            case THIS_YEAR -> {
                startDate = LocalDate.of(baseYear, 1, 1);
                endDate = LocalDate.of(baseYear, 12, 31);
            }
            case LAST_YEAR -> {
                startDate = LocalDate.of(baseYear - 1, 1, 1);
                endDate = LocalDate.of(baseYear - 1, 12, 31);
            }
            case BEFORE -> endDate = LocalDate.of(baseYear - 2, 12, 31);
            case ALL -> { /* 연도 제한 없음 */ }
            default -> throw new IllegalStateException("Unhandled HistoryYearFilter: " + appliedFilter);
        }

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findParticipationHistory(
                userId, appliedFilter, baseYear, startDate, endDate, pageNumber, pageSize);
    }

    // 관심 공연 목록 조회 (알림/참여 상태 포함, offset 무한스크롤)
    public InterestedPerformanceResponse getInterestedPerformances(Long userId, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findInterestedPerformances(userId, pageNumber, pageSize);
    }
}
