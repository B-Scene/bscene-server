package com.umc.bscene.domain.band.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import com.umc.bscene.domain.recommendation.event.BandRecommendationExposedEvent;
import com.umc.bscene.domain.recommendation.repository.BandSimilarityRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// v2 : 장르/지역 선호 + 최근 활동 + 팔로우 밴드 유사도 기반 룰 스코어링. 고정 상위 N개 (커서 페이지네이션 없음).
// 현재 이 구현체가 @Primary 로 실제 주입되는 대상이다. v1로 되돌리려면 @Primary를 BandRecommendationServiceV1Impl로 옮기면 된다.
@Primary
@Service("bandRecommendationServiceV2")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandRecommendationServiceV2Impl implements BandRecommendationService {

    private static final int RECOMMEND_LIMIT = 10;
    private static final int RECENT_ACTIVITY_DAYS = 30;

    // explicit(장르/지역/활동) 내부 가중치. 셋 다 만족 시 explicit 원점수는 GENRE+REGION+ACTIVITY(=6)로 정규화된다.
    private static final double GENRE_MATCH_SCORE = 3;
    private static final double REGION_MATCH_SCORE = 2;
    private static final double RECENT_ACTIVITY_SCORE = 1;
    private static final double EXPLICIT_MAX_SCORE = GENRE_MATCH_SCORE + REGION_MATCH_SCORE + RECENT_ACTIVITY_SCORE;

    // implicit(팔로우 밴드 유사도) 평균 정규화 기준값. 팔로우한 밴드 대비 평균 유사도가 이 값 이상이면 만점(1.0)으로 취급한다.
    private static final double SIMILARITY_AVG_CAP = 10.0;

    // explicit vs implicit 결합 가중치. 초기 단계(팔로우 그래프가 얕음)에서는 유저가 직접 선택한 explicit 신호를 더 신뢰한다.
    // implicit 데이터(팔로우/유사도)가 충분히 쌓이면 IMPLICIT_WEIGHT를 점진적으로 올리는 방향으로 재조정한다.
    private static final double EXPLICIT_WEIGHT = 0.6;
    private static final double IMPLICIT_WEIGHT = 0.4;

    private static final double SCORE_SCALE = 10.0;
    private static final String ALGORITHM_VERSION = "rule-v2";

    // reason 우선순위 : 유사도 > 장르 > 지역
    private static final String REASON_SIMILARITY = "팔로우한 밴드와 유사한 스타일";
    private static final String REASON_GENRE = "선호 장르 일치";
    private static final String REASON_REGION = "선호 지역 일치";

    private final BandRepository bandRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final PostRepository postRepository;
    private final BandSimilarityRepository bandSimilarityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size) {
        int limit = (size != null) ? Math.min(size, RECOMMEND_LIMIT) : RECOMMEND_LIMIT;

        User user = userRepository.getReferenceById(userId);

        Set<Genre> preferredGenres = userGenresRepository.findAllByUser(user).stream()
                .map(UserGenres::getGenre)
                .collect(Collectors.toSet());
        Set<Region> preferredRegions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .collect(Collectors.toSet());

        List<Long> followedBandIds = followRepository.findBandIdsByUserId(userId);
        Set<Long> followedBandIdSet = new HashSet<>(followedBandIds);

        Map<Long, Double> similarityScoreByBandId = followedBandIds.isEmpty()
                ? Map.of()
                : bandSimilarityRepository.findByBandIdIn(followedBandIds).stream()
                        .collect(Collectors.groupingBy(
                                bs -> bs.getSimilarBand().getId(),
                                Collectors.summingDouble(BandSimilarity::getScore)));

        Map<Long, Band> candidateBands = collectCandidateBands(preferredGenres, preferredRegions, similarityScoreByBandId);
        followedBandIdSet.forEach(candidateBands::remove);

        List<Long> candidateIds = new ArrayList<>(candidateBands.keySet());

        LocalDateTime activitySince = LocalDateTime.now().minusDays(RECENT_ACTIVITY_DAYS);
        Set<Long> recentActivityBandIds = candidateIds.isEmpty()
                ? Set.of()
                : new HashSet<>(postRepository.findBandIdsWithRecentPost(candidateIds, activitySince));

        Map<Long, LocalDateTime> latestActivityAtByBandId = candidateIds.isEmpty()
                ? Map.of()
                : postRepository.findLatestActivityAtByBandIds(candidateIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        int followedCount = followedBandIdSet.size();

        List<ScoredBand> scored = candidateBands.values().stream()
                .map(band -> score(
                        band, preferredGenres, preferredRegions, followedCount,
                        recentActivityBandIds, similarityScoreByBandId, latestActivityAtByBandId))
                .sorted(Comparator.comparingDouble(ScoredBand::score).reversed()
                        .thenComparing(ScoredBand::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();

        List<BandRecommendItem> items = scored.stream()
                .map(sb -> BandRecommendItem.of(sb.band(), sb.score(), sb.reason()))
                .toList();

        publishExposedEvent(userId, items);

        return BandRecommendResponse.of(items, false, null);
    }

    // 장르/지역 선호 일치 밴드 + 팔로우 밴드와 유사한 밴드를 후보군으로 모은다 (전체 밴드 스캔 방지)
    private Map<Long, Band> collectCandidateBands(
            Set<Genre> preferredGenres,
            Set<Region> preferredRegions,
            Map<Long, Double> similarityScoreByBandId
    ) {
        Map<Long, Band> candidateBands = new LinkedHashMap<>();

        if (!preferredGenres.isEmpty()) {
            bandRepository.findByGenreIn(preferredGenres)
                    .forEach(band -> candidateBands.put(band.getId(), band));
        }
        if (!preferredRegions.isEmpty()) {
            bandRepository.findByRegionIn(preferredRegions)
                    .forEach(band -> candidateBands.put(band.getId(), band));
        }
        if (!similarityScoreByBandId.isEmpty()) {
            bandRepository.findAllById(similarityScoreByBandId.keySet())
                    .forEach(band -> candidateBands.put(band.getId(), band));
        }

        return candidateBands;
    }

    private ScoredBand score(
            Band band,
            Set<Genre> preferredGenres,
            Set<Region> preferredRegions,
            int followedCount,
            Set<Long> recentActivityBandIds,
            Map<Long, Double> similarityScoreByBandId,
            Map<Long, LocalDateTime> latestActivityAtByBandId
    ) {
        double genreScore = preferredGenres.contains(band.getGenre()) ? GENRE_MATCH_SCORE : 0;
        double regionScore = preferredRegions.contains(band.getRegion()) ? REGION_MATCH_SCORE : 0;
        double activityScore = recentActivityBandIds.contains(band.getId()) ? RECENT_ACTIVITY_SCORE : 0;
        double explicitNorm = (genreScore + regionScore + activityScore) / EXPLICIT_MAX_SCORE;

        // 팔로우 개수로 나눠 평균을 내므로, 팔로우를 많이 한 유저일수록 유사도 총합이 부풀려지는 편향을 없앤다.
        double similarityRaw = similarityScoreByBandId.getOrDefault(band.getId(), 0.0);
        double similarityAvg = followedCount == 0 ? 0 : similarityRaw / followedCount;
        double implicitNorm = Math.min(similarityAvg, SIMILARITY_AVG_CAP) / SIMILARITY_AVG_CAP;

        double totalScore = (EXPLICIT_WEIGHT * explicitNorm + IMPLICIT_WEIGHT * implicitNorm)
                / (EXPLICIT_WEIGHT + IMPLICIT_WEIGHT) * SCORE_SCALE;

        double genreContribution = EXPLICIT_WEIGHT * (genreScore / EXPLICIT_MAX_SCORE);
        double regionContribution = EXPLICIT_WEIGHT * (regionScore / EXPLICIT_MAX_SCORE);
        double similarityContribution = IMPLICIT_WEIGHT * implicitNorm;
        String reason = determineReason(similarityContribution, genreContribution, regionContribution);
        LocalDateTime lastActivityAt = latestActivityAtByBandId.get(band.getId());

        return new ScoredBand(band, totalScore, lastActivityAt, reason);
    }

    // 동점 기여 요인 우선순위 : 유사도 > 장르 > 지역
    private String determineReason(double similarityContribution, double genreContribution, double regionContribution) {
        double max = Math.max(similarityContribution, Math.max(genreContribution, regionContribution));
        if (max <= 0) {
            return null;
        }
        if (similarityContribution == max) {
            return REASON_SIMILARITY;
        }
        if (genreContribution == max) {
            return REASON_GENRE;
        }
        return REASON_REGION;
    }

    private void publishExposedEvent(Long userId, List<BandRecommendItem> items) {
        List<BandRecommendationExposedEvent.Exposure> exposures = IntStream.range(0, items.size())
                .mapToObj(i -> new BandRecommendationExposedEvent.Exposure(
                        items.get(i).bandId(), i + 1, ALGORITHM_VERSION))
                .toList();
        eventPublisher.publishEvent(new BandRecommendationExposedEvent(userId, exposures));
    }

    private record ScoredBand(Band band, double score, LocalDateTime lastActivityAt, String reason) {
    }
}
