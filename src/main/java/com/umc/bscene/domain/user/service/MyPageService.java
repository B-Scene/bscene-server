package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.dto.response.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

        // 온보딩 장르는 선택 순서(PK순)로 조회 — 폴백과 동점 처리 기준으로 사용
        List<Genre> onboardingGenres = userGenresRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(UserGenres::getGenre)
                .toList();

        List<Region> regions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .toList();

        Long userId = user.getId();

        // 대표 장르 : 팔로우한 밴드들의 최다 장르 / 팔로우가 없으면 온보딩 첫 장르로 폴백
        Map<Genre, Long> genreCounts = followPort.countFollowedBandsGroupedByGenre(userId);
        Genre genre;
        int additionalGenreCount;
        if (genreCounts.isEmpty()) {
            genre = onboardingGenres.isEmpty() ? null : onboardingGenres.getFirst();
            additionalGenreCount = onboardingGenres.isEmpty() ? 0 : onboardingGenres.size() - 1;
        } else {
            genre = pickRepresentativeGenre(genreCounts, onboardingGenres);
            additionalGenreCount = genreCounts.size() - 1;
        }

        long followingCount = followPort.countFollowing(userId);
        long interestedPerformanceCount = performancePort.countInterested(userId);
        long participatedPerformanceCount = performancePort.countParticipated(userId);

        return FanMyPageResponse.of(
                nickname,
                genre,
                additionalGenreCount,
                regions,
                user.getCurrentMode(),
                followingCount,
                interestedPerformanceCount,
                participatedPerformanceCount
        );
    }

    // 밴드 수 최다 장르 → 동점이면 온보딩에서 먼저 고른 장르 → 그것도 아니면 이름순 (조회마다 값이 흔들리지 않게 결정적 선택)
    private Genre pickRepresentativeGenre(Map<Genre, Long> genreCounts, List<Genre> onboardingGenres) {
        return genreCounts.entrySet().stream()
                .min(Comparator
                        .comparingLong((Map.Entry<Genre, Long> entry) -> -entry.getValue())
                        .thenComparingInt(entry -> onboardingRank(onboardingGenres, entry.getKey()))
                        .thenComparing(entry -> entry.getKey().name()))
                .map(Map.Entry::getKey)
                .orElseThrow();     // 호출부에서 빈 Map은 폴백으로 걸러지므로 도달하지 않음
    }

    // 온보딩에서 고른 순서 (없는 장르는 맨 뒤)
    private int onboardingRank(List<Genre> onboardingGenres, Genre genre) {
        int index = onboardingGenres.indexOf(genre);
        return (index == -1) ? Integer.MAX_VALUE : index;
    }

    // 공연 참여 기록 조회 (참여 완료 공연, 연도 필터, offset 무한스크롤)
    // 필터는 서버 기준 올해에 상대적 : THIS_YEAR(올해) / LAST_YEAR(작년) / BEFORE(재작년 이전) / ALL(전체)
    public ParticipationHistoryResponse getParticipationHistory(
            Long userId, HistoryYearFilter filter, int page, int size) {
        HistoryYearFilter appliedFilter = (filter == null) ? HistoryYearFilter.ALL : filter;
        int baseYear = LocalDate.now().getYear();
        YearDateRange range = resolveYearDateRange(appliedFilter, baseYear);

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findParticipationHistory(
                userId, appliedFilter, baseYear, range.startDate(), range.endDate(), pageNumber, pageSize);
    }

    // 관심 공연 목록 조회 (알림/참여 상태 포함, 연도 필터, offset 무한스크롤)
    // 필터는 참여 기록 조회와 동일 : THIS_YEAR(올해) / LAST_YEAR(작년) / BEFORE(재작년 이전) / ALL(전체)
    public InterestedPerformanceResponse getInterestedPerformances(
            Long userId, HistoryYearFilter filter, int page, int size) {
        HistoryYearFilter appliedFilter = (filter == null) ? HistoryYearFilter.ALL : filter;
        int baseYear = LocalDate.now().getYear();
        YearDateRange range = resolveYearDateRange(appliedFilter, baseYear);

        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return performancePort.findInterestedPerformances(
                userId, appliedFilter, baseYear, range.startDate(), range.endDate(), pageNumber, pageSize);
    }

    // 연도 필터 → 조회 날짜 범위 (null이면 해당 방향 제한 없음)
    private record YearDateRange(LocalDate startDate, LocalDate endDate) {
    }

    private YearDateRange resolveYearDateRange(HistoryYearFilter filter, int baseYear) {
        return switch (filter) {
            case THIS_YEAR -> new YearDateRange(LocalDate.of(baseYear, 1, 1), LocalDate.of(baseYear, 12, 31));
            case LAST_YEAR -> new YearDateRange(LocalDate.of(baseYear - 1, 1, 1), LocalDate.of(baseYear - 1, 12, 31));
            case BEFORE -> new YearDateRange(null, LocalDate.of(baseYear - 2, 12, 31));
            case ALL -> new YearDateRange(null, null);
        };
    }

    // 팔로우한 밴드 목록 조회 (밴드명 가나다순, offset 무한스크롤)
    public FollowedBandResponse getFollowedBands(Long userId, int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return followPort.findFollowedBands(userId, pageNumber, pageSize);
    }
}
