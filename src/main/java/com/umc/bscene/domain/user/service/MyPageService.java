package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.dto.request.MyInfoUpdateRequest;
import com.umc.bscene.domain.user.dto.response.FanMyPageResponse;
import com.umc.bscene.domain.user.dto.response.FollowedBandResponse;
import com.umc.bscene.domain.user.dto.response.InterestedPerformanceResponse;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
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
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final UserRepository userRepository;
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

        // 등록한 관심장르를 선택 순서(PK순)로 조회 — 대표 장르 후보 목록이자 동점 처리 기준
        List<Genre> onboardingGenres = userGenresRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(UserGenres::getGenre)
                .toList();

        List<Region> regions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .toList();

        Long userId = user.getId();

        // 대표 장르 : 사용자가 등록한 관심장르 중에서 선정 (등록하지 않은 장르는 팔로우한 밴드가 많아도 후보에서 제외)
        // additionalGenreCount는 대표를 제외한 나머지 관심장르 수 → 화면의 "○○ 외 N개"
        Map<Genre, Long> followedGenreCounts = followPort.countFollowedBandsGroupedByGenre(userId);
        Genre genre = pickRepresentativeGenre(onboardingGenres, followedGenreCounts);
        int additionalGenreCount = Math.max(onboardingGenres.size() - 1, 0);

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

    // 관심장르 중 팔로우한 밴드가 많은 장르 → 동점이거나 팔로우가 없으면 먼저 고른 관심장르 (조회마다 값이 흔들리지 않게 결정적 선택)
    private Genre pickRepresentativeGenre(List<Genre> onboardingGenres, Map<Genre, Long> followedGenreCounts) {
        return onboardingGenres.stream()
                .min(Comparator
                        .comparingLong((Genre genre) -> -followedGenreCounts.getOrDefault(genre, 0L))
                        .thenComparingInt(onboardingGenres::indexOf))
                .orElse(null);      // 관심장르가 비어있으면 대표 장르 없음 (온보딩 필수라 정상 흐름에선 도달하지 않음)
    }

    // 내 정보 조회 (내 정보 수정 화면 초기값 : 닉네임/관심 장르/활동 지역)
    public MyInfoResponse getMyInfo(User user) {
        FanProfile fanProfile = fanProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));

        return buildMyInfo(fanProfile, user);
    }

    // 내 정보 수정 (온보딩에서 설정한 닉네임/관심 장르/활동 지역을 통째로 교체)
    @Transactional
    public MyInfoResponse updateMyInfo(Long userId, MyInfoUpdateRequest request) {
        // 인증 필터에서 로드된 User는 detached 상태 → 변경 감지를 위해 트랜잭션 내에서 재조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        FanProfile fanProfile = fanProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserException(UserErrorCode.FAN_PROFILE_NOT_FOUND));

        // 닉네임 중복 검사는 본인 프로필 제외 (본인이 쓰던 닉네임 그대로 저장하는 건 허용)
        String nickname = request.nickname();
        if (fanProfileRepository.existsByNicknameAndUser_IdNot(nickname, userId)) {
            throw new UserException(UserErrorCode.DUPLICATE_NICKNAME);
        }
        fanProfile.updateNickname(nickname);

        // 검사와 커밋 사이에 다른 유저가 같은 닉네임을 저장하는 race 대비 :
        // 닉네임 UPDATE를 미리 flush해서 unique 제약 위반이면 500 대신 409로 응답
        try {
            fanProfileRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 장르/지역은 온보딩과 동일하게 전체 삭제 후 재저장
        // (user, genre) unique 제약이 있는데 Hibernate는 insert를 delete보다 먼저 실행하므로 삭제를 먼저 flush
        userGenresRepository.deleteAllByUser(user);
        userGenresRepository.flush();
        List<UserGenres> genres = request.genres().stream()
                .distinct()
                .map(genre -> UserGenres.builder().user(user).genre(genre).build())
                .toList();
        userGenresRepository.saveAll(genres);

        userRegionsRepository.deleteAllByUser(user);
        userRegionsRepository.flush();
        List<UserRegions> regions = request.regions().stream()
                .distinct()
                .map(region -> UserRegions.builder().user(user).region(region).build())
                .toList();
        userRegionsRepository.saveAll(regions);

        return buildMyInfo(fanProfile, user);
    }

    // 장르/지역은 enum code만 내려줌 (한글명 매칭은 /genres, /regions 목록 조회 응답 사용)
    private MyInfoResponse buildMyInfo(FanProfile fanProfile, User user) {
        List<String> genres = userGenresRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(userGenre -> userGenre.getGenre().name())
                .toList();

        List<String> regions = userRegionsRepository.findAllByUserOrderByIdAsc(user).stream()
                .map(userRegion -> userRegion.getRegion().name())
                .toList();

        return new MyInfoResponse(fanProfile.getNickname(), fanProfile.getProfileImageUrl(), genres, regions);
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
