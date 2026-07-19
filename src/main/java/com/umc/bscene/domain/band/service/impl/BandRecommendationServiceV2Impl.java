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
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// v2 : 장르/지역 선호 + 최근 활동 + 팔로우/관심(클릭) 밴드 유사도 기반
@Primary
@Service("bandRecommendationServiceV2")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandRecommendationServiceV2Impl implements BandRecommendationService {

    private static final int RECOMMEND_LIMIT = 10;
    private static final int RECENT_ACTIVITY_DAYS = 30;
    private static final int TOP_CLICKED_BANDS_LIMIT = 10;

    // explicit(장르/지역/활동) 내부 가중치. 셋 다 만족 시 explicit 원점수는 GENRE+REGION+ACTIVITY(=6)로 정규화된다.
    private static final double GENRE_MATCH_SCORE = 3;
    private static final double REGION_MATCH_SCORE = 2;
    private static final double RECENT_ACTIVITY_SCORE = 1;
    private static final double EXPLICIT_MAX_SCORE = GENRE_MATCH_SCORE + REGION_MATCH_SCORE + RECENT_ACTIVITY_SCORE;

    private static final double SIMILARITY_AVG_CAP = 0.8;

    // 유사도 시드 종류별 가중치. 팔로우는 유저가 직접 선택한 강한 신호, 클릭은 관심을 추정만 한 약한 신호라 절반만 반영한다.
    private static final double FOLLOW_SEED_WEIGHT = 1.0;
    private static final double CLICK_SEED_WEIGHT = 0.5;

    // implicit 데이터(팔로우/유사도)가 충분히 쌓이면 IMPLICIT_WEIGHT를 점진적으로 올리는 방향으로 재조정한다.
    private static final double EXPLICIT_WEIGHT = 0.6;
    private static final double IMPLICIT_WEIGHT = 0.4;

    private static final double SCORE_SCALE = 10.0;
    private static final String ALGORITHM_VERSION = "rule-v2";

    // reason 우선순위 : 유사도 > 장르 > 지역 > 최근 활동 (배점 GENRE(3) > REGION(2) > ACTIVITY(1) 순이라 동점은 거의 안 생김)
    private static final String REASON_SIMILARITY_FOLLOW = "팔로우한 밴드와 유사한 스타일";
    private static final String REASON_SIMILARITY_CLICK = "관심 있게 본 밴드와 유사한 스타일";
    private static final String REASON_GENRE = "선호 장르 일치";
    private static final String REASON_REGION = "선호 지역 일치";
    private static final String REASON_ACTIVITY = "최근 활동 있는 밴드";

    private final BandRepository bandRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final PostRepository postRepository;
    private final BandSimilarityRepository bandSimilarityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BandInteractionRepository bandInteractionRepository;

    @Override
    public BandRecommendResponse getRecommendedBands(Long userId, Long cursor, Integer size) {
        int limit = (size != null) ? Math.min(size, RECOMMEND_LIMIT) : RECOMMEND_LIMIT;

        User user = userRepository.getReferenceById(userId);

        Set<Genre> preferredGenres = userGenresRepository.findAllByUser(user).stream()
                .map(UserGenres::getGenre)
                .collect(Collectors.toSet());

        Set<Region> preferredRegions = userRegionsRepository.findAllByUser(user).stream()
                .map(UserRegions::getRegion)
                .filter(region -> region != Region.UNKNOWN)
                .collect(Collectors.toSet());

        List<Long> followedBandIds = followRepository.findBandIdsByUserId(userId);
        Set<Long> followedBandIdSet = new HashSet<>(followedBandIds);

        List<Long> clickedBandIds = bandInteractionRepository
                .findByUser_IdOrderByClickCountDesc(userId, PageRequest.of(0, TOP_CLICKED_BANDS_LIMIT))
                .stream()
                .map(bi -> bi.getBand().getId())
                .toList();

        // 팔로우 밴드 + 자주 클릭한(관심) 밴드를 합쳐 유사도 조회 시드로 사용한다.
        Set<Long> similaritySeedBandIds = new LinkedHashSet<>(followedBandIds);
        similaritySeedBandIds.addAll(clickedBandIds);

        List<BandSimilarity> similarityRows = similaritySeedBandIds.isEmpty()
                ? List.of()
                : bandSimilarityRepository.findByBandIdIn(new ArrayList<>(similaritySeedBandIds));


        Map<Long, Double> similarityWeightedSumByBandId = similarityRows.stream()
                .collect(Collectors.groupingBy(
                        bs -> bs.getSimilarBand().getId(),
                        Collectors.summingDouble(bs -> bs.getScore() * seedWeight(bs, followedBandIdSet))));

        Map<Long, Long> similarityMatchCountByBandId = similarityRows.stream()
                .collect(Collectors.groupingBy(bs -> bs.getSimilarBand().getId(), Collectors.counting()));

        // reason 문구 결정용 - 팔로우 시드로부터도 유사도를 받은 후보는 팔로우 문구를 우선한다.
        Set<Long> followSimilarBandIds = similarityRows.stream()
                .filter(bs -> followedBandIdSet.contains(bs.getBand().getId()))
                .map(bs -> bs.getSimilarBand().getId())
                .collect(Collectors.toSet());

        Map<Long, Band> candidateBands = collectCandidateBands(preferredGenres, preferredRegions, similarityWeightedSumByBandId);
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

        List<ScoredBand> scored = candidateBands.values().stream()
                .map(band -> score(
                        band, preferredGenres, preferredRegions, recentActivityBandIds,
                        similarityWeightedSumByBandId, similarityMatchCountByBandId, followSimilarBandIds,
                        latestActivityAtByBandId))
                .sorted(Comparator.comparingDouble(ScoredBand::score).reversed()
                        .thenComparing(ScoredBand::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = scored.size();
        long offset = (cursor != null) ? cursor : 0;
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + limit, total);

        List<BandRecommendItem> items = scored.subList(fromIndex, toIndex).stream()
                .map(sb -> BandRecommendItem.of(sb.band(), sb.score(), sb.reason()))
                .toList();

        boolean hasNext = toIndex < total;
        Long nextCursor = hasNext ? (long) toIndex : null;

        publishExposedEvent(userId, items, fromIndex);

        return BandRecommendResponse.of(items, hasNext, nextCursor);
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
            Set<Long> recentActivityBandIds,
            Map<Long, Double> similarityWeightedSumByBandId,
            Map<Long, Long> similarityMatchCountByBandId,
            Set<Long> followSimilarBandIds,
            Map<Long, LocalDateTime> latestActivityAtByBandId
    ) {
        double genreScore = preferredGenres.contains(band.getGenre()) ? GENRE_MATCH_SCORE : 0;
        double regionScore = preferredRegions.contains(band.getRegion()) ? REGION_MATCH_SCORE : 0;
        double activityScore = recentActivityBandIds.contains(band.getId()) ? RECENT_ACTIVITY_SCORE : 0;
        double explicitNorm = (genreScore + regionScore + activityScore) / EXPLICIT_MAX_SCORE;

        double weightedSum = similarityWeightedSumByBandId.getOrDefault(band.getId(), 0.0);
        long matchCount = similarityMatchCountByBandId.getOrDefault(band.getId(), 0L);
        double similarityAvg = matchCount == 0 ? 0 : weightedSum / matchCount;
        double implicitNorm = Math.min(similarityAvg, SIMILARITY_AVG_CAP) / SIMILARITY_AVG_CAP;

        double totalScore = (EXPLICIT_WEIGHT * explicitNorm + IMPLICIT_WEIGHT * implicitNorm)
                / (EXPLICIT_WEIGHT + IMPLICIT_WEIGHT) * SCORE_SCALE;

        double genreContribution = EXPLICIT_WEIGHT * (genreScore / EXPLICIT_MAX_SCORE);
        double regionContribution = EXPLICIT_WEIGHT * (regionScore / EXPLICIT_MAX_SCORE);
        double activityContribution = EXPLICIT_WEIGHT * (activityScore / EXPLICIT_MAX_SCORE);
        double similarityContribution = IMPLICIT_WEIGHT * implicitNorm;
        boolean similarityFromFollow = followSimilarBandIds.contains(band.getId());
        String reason = determineReason(
                similarityContribution, genreContribution, regionContribution, activityContribution, similarityFromFollow);
        LocalDateTime lastActivityAt = latestActivityAtByBandId.get(band.getId());

        return new ScoredBand(band, totalScore, lastActivityAt, reason);
    }

    // 팔로우/클릭 시드 중 어느 쪽에서 온 유사도 행인지에 따른 가중치
    private double seedWeight(BandSimilarity bandSimilarity, Set<Long> followedBandIdSet) {
        return followedBandIdSet.contains(bandSimilarity.getBand().getId()) ? FOLLOW_SEED_WEIGHT : CLICK_SEED_WEIGHT;
    }

    // 동점 기여 요인 우선순위 : 유사도 > 장르 > 지역 > 최근 활동. 유사도가 1등이면 팔로우/클릭 중 어느 시드에서 왔는지로 문구를 나눈다.
    private String determineReason(
            double similarityContribution,
            double genreContribution,
            double regionContribution,
            double activityContribution,
            boolean similarityFromFollow
    ) {
        double max = Math.max(
                similarityContribution,
                Math.max(genreContribution, Math.max(regionContribution, activityContribution)));
        if (max <= 0) {
            return null;
        }
        if (similarityContribution == max) {
            return similarityFromFollow ? REASON_SIMILARITY_FOLLOW : REASON_SIMILARITY_CLICK;
        }
        if (genreContribution == max) {
            return REASON_GENRE;
        }
        if (regionContribution == max) {
            return REASON_REGION;
        }
        return REASON_ACTIVITY;
    }

    private void publishExposedEvent(Long userId, List<BandRecommendItem> items, int positionOffset) {
        List<BandRecommendationExposedEvent.Exposure> exposures = IntStream.range(0, items.size())
                .mapToObj(i -> new BandRecommendationExposedEvent.Exposure(
                        items.get(i).bandId(), positionOffset + i + 1, ALGORITHM_VERSION))
                .toList();
        eventPublisher.publishEvent(new BandRecommendationExposedEvent(userId, exposures));
    }

    private record ScoredBand(Band band, double score, LocalDateTime lastActivityAt, String reason) {
    }
}
