package com.umc.bscene.domain.band.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// v1 : 장르/지역 선호 + 최근 활동(포스트·공연) 기반 룰 스코어링. 커서 기반 페이지네이션.
@Service("bandRecommendationServiceV1")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandRecommendationServiceV1Impl implements BandRecommendationService {

    // 밴드 추천 스코어링 기준
    private static final int DEFAULT_RECOMMEND_PAGE_SIZE = 5;
    private static final int MIN_RECOMMEND_SCORE = 3;
    private static final int RECENT_ACTIVITY_DAYS = 30;
    private static final int GENRE_MATCH_SCORE = 3;
    private static final int REGION_MATCH_SCORE = 2;
    private static final int RECENT_POST_SCORE = 2;
    private static final int RECENT_PERFORMANCE_SCORE = 1;

    // 더미 밴드 경계 : 운영 DB 시드 기준으로 이 값 이하 band id는 전부 더미 밴드 (withDummy=false 필터의 기준)
    private static final long DUMMY_BAND_MAX_ID = 474;

    private final BandRepository bandRepository;
    private final UserRepository userRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final PostRepository postRepository;
    private final PerformanceRepository performanceRepository;

    // 밴드 추천 목록 조회 (장르/지역/활동 내역 기반 스코어링)
    @Override
    public BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size, boolean withDummy) {
        int pageSize = (size != null) ? size : DEFAULT_RECOMMEND_PAGE_SIZE;
        long offset = (cursor != null) ? cursor : 0;

        User user = userRepository.getReferenceById(userId);

        Set<Genre> preferredGenres = userGenresRepository.findAllByUser(user).stream()
                .map(UserGenres::getGenre)
                .collect(Collectors.toSet());

        Set<Region> preferredRegions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .collect(Collectors.toSet());

        List<Band> candidates = bandRepository.findAllByStatus(BandStatus.ACCEPTED).stream()
                .filter(band -> withDummy || band.getId() > DUMMY_BAND_MAX_ID)
                .toList();
        List<Long> candidateIds = candidates.stream().map(Band::getId).toList();

        LocalDateTime postSince = LocalDateTime.now().minusDays(RECENT_ACTIVITY_DAYS);
        LocalDate performanceSince = LocalDate.now().minusDays(RECENT_ACTIVITY_DAYS);

        Set<Long> recentPostBandIds = candidateIds.isEmpty()
                ? Set.of()
                : new HashSet<>(postRepository.findBandIdsWithRecentPost(candidateIds, postSince));

        Set<Long> recentPerformanceBandIds = candidateIds.isEmpty()
                ? Set.of()
                : new HashSet<>(performanceRepository.findBandIdsWithRecentPerformance(
                        candidateIds, PerformanceStatus.ACTIVE, performanceSince));

        List<BandRecommendItem> scored = candidates.stream()
                .map(band -> BandRecommendItem.of(band, calculateScore(
                        band, preferredGenres, preferredRegions, recentPostBandIds, recentPerformanceBandIds), null))
                .filter(item -> item.score() >= MIN_RECOMMEND_SCORE)
                .sorted(Comparator.comparingDouble(BandRecommendItem::score).reversed()
                        .thenComparing(Comparator.comparingLong(BandRecommendItem::bandId).reversed()))
                .toList();

        int total = scored.size();
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + pageSize, total);
        List<BandRecommendItem> page = scored.subList(fromIndex, toIndex);

        boolean hasNext = toIndex < total;
        Long nextCursor = hasNext ? (long) toIndex : null;

        return BandRecommendResponse.of(page, hasNext, nextCursor);
    }

    private int calculateScore(
            Band band,
            Set<Genre> preferredGenres,
            Set<Region> preferredRegions,
            Set<Long> recentPostBandIds,
            Set<Long> recentPerformanceBandIds
    ) {
        int score = 0;
        if (preferredGenres.contains(band.getGenre())) score += GENRE_MATCH_SCORE;
        if (preferredRegions.contains(band.getRegion())) score += REGION_MATCH_SCORE;
        if (recentPostBandIds.contains(band.getId())) score += RECENT_POST_SCORE;
        if (recentPerformanceBandIds.contains(band.getId())) score += RECENT_PERFORMANCE_SCORE;
        return score;
    }
}
