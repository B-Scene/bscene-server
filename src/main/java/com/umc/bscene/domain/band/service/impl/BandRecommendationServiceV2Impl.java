package com.umc.bscene.domain.band.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.BandRecommendationService;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import com.umc.bscene.domain.recommendation.event.BandRecommendationExposedEvent;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import com.umc.bscene.domain.recommendation.repository.BandRecommendationLogRepository;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// v2 : 장르/지역 선호 + 최근 활동 + 팔로우/관심(클릭) 밴드 유사도 기반 + 콜드스타트 폴백/신생 밴드 부스트/노출 피로도 감점
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

    // 인기도(팔로워) 항목. 절대 팔로워수는 로그로 압축해 신인/인기 밴드 간 격차를 완만하게 만들고,
    // 최근 성장세(신규 팔로워)에 더 큰 비중을 두었다.
    // 로그 캡은 고정값이 아니라 이번 추천 후보군 내 최댓값 기준으로 매 요청마다 동적으로 잡는다.
    private static final int RECENT_FOLLOWER_GROWTH_DAYS = 7;
    private static final long POPULARITY_TOTAL_FLOOR = 20;
    private static final long POPULARITY_GROWTH_FLOOR = 5;
    private static final double POPULARITY_TOTAL_RATIO = 0.3;
    private static final double POPULARITY_GROWTH_RATIO = 0.7;
    private static final double POPULARITY_WEIGHT = 0.5;

    // 콜드스타트 폴백 : 장르/지역 선호도, 팔로우, 클릭 이력이 전부 없어 후보군이 비는 유저(신규 가입 등)에게
    // 채워줄 풀 크기. 팔로워 상위 밴드로 우선 채우고, 모자라면 최근 생성 밴드로 보충한다.
    private static final int COLD_START_FALLBACK_POOL_SIZE = 30;

    // 신생 밴드 노출 부스트 : 팔로우/활동 이력이 없는 신생 밴드가 항상 최하위로 밀리는 걸 막기 위해
    // 생성 후 1주일간만 아주 소폭의 가산점을 준다. 생성일로부터 선형 감쇠
    private static final int NEW_BAND_BOOST_DAYS = 7;
    private static final double NEW_BAND_BOOST_WEIGHT = 0.05;

    // 노출 피로도 감점 : 후보에서 완전히 빼지 않고 점수만 깎는다.
    private static final int EXPOSURE_VISIBLE_POSITION_LIMIT = 10;
    private static final double EXPOSURE_HALF_LIFE_DAYS = 3.0;

    // half-life 7번(2^-7≈0.78%)이면 감쇠가 사실상 0에 수렴한다고 보고 조회 컷오프로 삼는다.
    private static final int EXPOSURE_FATIGUE_LOOKBACK_DAYS = (int) Math.ceil(EXPOSURE_HALF_LIFE_DAYS * 7);
    private static final double EXPOSURE_GRACE = 2.0; // 이 유효노출 수까지는 무페널티
    private static final double EXPOSURE_PENALTY_ALPHA = 0.5;
    private static final double EXPOSURE_PENALTY_FLOOR = 0.25;
    private static final double LN2 = Math.log(2);

    // reason 우선순위 : 유사도 > 장르 > 지역 > 최근 활동 (배점 GENRE(3) > REGION(2) > ACTIVITY(1) 순이라 동점은 거의 안 생김)
    // 콜드스타트 폴백 밴드는 위 신호가 전부 0이라, 폴백 출처(신규/인기)로 reason을 대신 채운다.
    private static final String REASON_SIMILARITY_FOLLOW = "팔로우한 밴드와 유사한 스타일";
    private static final String REASON_SIMILARITY_CLICK = "관심 있게 본 밴드와 유사한 스타일";
    private static final String REASON_GENRE = "선호 장르 일치";
    private static final String REASON_REGION = "선호 지역 일치";
    private static final String REASON_ACTIVITY = "최근 활동 있는 밴드";
    private static final String REASON_NEW_BAND = "새로 등록된 밴드";
    private static final String REASON_POPULAR = "요즘 인기있는 밴드";

    private final BandRepository bandRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;
    private final PostRepository postRepository;
    private final PerformanceRepository performanceRepository;
    private final BandSimilarityRepository bandSimilarityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BandInteractionRepository bandInteractionRepository;
    private final BandRecommendationLogRepository bandRecommendationLogRepository;

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

        // 콜드스타트 폴백 : 장르/지역/유사도 어느 쪽으로도 후보가 안 나오면 인기/신생 밴드로 채운다.
        ColdStartFallback coldStartFallback = candidateBands.isEmpty()
                ? collectColdStartFallback(followedBandIdSet)
                : ColdStartFallback.EMPTY;
        coldStartFallback.bands().forEach(band -> candidateBands.put(band.getId(), band));

        List<Long> candidateIds = new ArrayList<>(candidateBands.keySet());

        // 노출 피로도 감점 : 후보에서 빼지 않고 점수만 깎는다 (FLOOR 덕에 유일한 매치는 그대로 남음).
        Map<Long, Double> fatiguePenaltyByBandId = computeFatiguePenalties(userId, candidateIds);

        // 최근 활동 = 최근 포스트 작성 OR 최근/예정 공연 (둘 중 하나만 있어도 활동 있는 것으로 취급)
        LocalDateTime postActivitySince = LocalDateTime.now().minusDays(RECENT_ACTIVITY_DAYS);
        LocalDate performanceActivitySince = LocalDate.now().minusDays(RECENT_ACTIVITY_DAYS);
        Set<Long> recentActivityBandIds = new HashSet<>();
        if (!candidateIds.isEmpty()) {
            recentActivityBandIds.addAll(postRepository.findBandIdsWithRecentPost(candidateIds, postActivitySince));
            recentActivityBandIds.addAll(performanceRepository.findBandIdsWithRecentPerformance(
                    candidateIds, PerformanceStatus.ACTIVE, performanceActivitySince));
        }

        Map<Long, LocalDateTime> latestActivityAtByBandId = candidateIds.isEmpty()
                ? Map.of()
                : postRepository.findLatestActivityAtByBandIds(candidateIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        LocalDateTime followGrowthSince = LocalDateTime.now().minusDays(RECENT_FOLLOWER_GROWTH_DAYS);
        Map<Long, Long> followerCountByBandId = candidateIds.isEmpty()
                ? Map.of()
                : followRepository.countFollowersByBandIds(candidateIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, Long> recentFollowerCountByBandId = candidateIds.isEmpty()
                ? Map.of()
                : followRepository.countRecentFollowersByBandIdIn(candidateIds, followGrowthSince).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        // 이번 추천 후보군 내 최댓값 기준으로 인기도 로그 캡을 동적으로 산출 (팔로우 데이터 규모에 따라 자동 스케일)
        long maxFollowerCount = followerCountByBandId.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        long maxRecentFollowerCount = recentFollowerCountByBandId.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        double totalLogCap = Math.log(Math.max(maxFollowerCount, POPULARITY_TOTAL_FLOOR) + 1);
        double growthLogCap = Math.log(Math.max(maxRecentFollowerCount, POPULARITY_GROWTH_FLOOR) + 1);

        List<ScoredBand> scored = candidateBands.values().stream()
                .map(band -> score(
                        band, preferredGenres, preferredRegions, recentActivityBandIds,
                        similarityWeightedSumByBandId, similarityMatchCountByBandId, followSimilarBandIds,
                        latestActivityAtByBandId, followerCountByBandId, recentFollowerCountByBandId,
                        totalLogCap, growthLogCap, coldStartFallback, fatiguePenaltyByBandId))
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
            bandRepository.findAllByIdInAndStatus(similarityScoreByBandId.keySet(), BandStatus.ACCEPTED)
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
            Map<Long, LocalDateTime> latestActivityAtByBandId,
            Map<Long, Long> followerCountByBandId,
            Map<Long, Long> recentFollowerCountByBandId,
            double totalLogCap,
            double growthLogCap,
            ColdStartFallback coldStartFallback,
            Map<Long, Double> fatiguePenaltyByBandId
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

        long followerCount = followerCountByBandId.getOrDefault(band.getId(), 0L);
        long recentFollowerCount = recentFollowerCountByBandId.getOrDefault(band.getId(), 0L);
        double totalNorm = Math.min(Math.log(followerCount + 1) / totalLogCap, 1.0);
        double growthNorm = Math.min(Math.log(recentFollowerCount + 1) / growthLogCap, 1.0);
        double popularityNorm = totalNorm * POPULARITY_TOTAL_RATIO + growthNorm * POPULARITY_GROWTH_RATIO;
        totalScore += popularityNorm * POPULARITY_WEIGHT;
        totalScore += calculateNewBandBoost(band.getCreatedAt());
        totalScore *= fatiguePenaltyByBandId.getOrDefault(band.getId(), 1.0);

        double genreContribution = EXPLICIT_WEIGHT * (genreScore / EXPLICIT_MAX_SCORE);
        double regionContribution = EXPLICIT_WEIGHT * (regionScore / EXPLICIT_MAX_SCORE);
        double activityContribution = EXPLICIT_WEIGHT * (activityScore / EXPLICIT_MAX_SCORE);
        double similarityContribution = IMPLICIT_WEIGHT * implicitNorm;
        boolean similarityFromFollow = followSimilarBandIds.contains(band.getId());
        String reason = determineReason(
                similarityContribution, genreContribution, regionContribution, activityContribution,
                similarityFromFollow, band.getId(), coldStartFallback);
        LocalDateTime lastActivityAt = latestActivityAtByBandId.get(band.getId());

        return new ScoredBand(band, totalScore, lastActivityAt, reason);
    }

    // 생성 후 NEW_BAND_BOOST_DAYS 이내인 밴드에게 선형 감쇠하는 가산점을 준다
    private double calculateNewBandBoost(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }
        long ageDays = Duration.between(createdAt, LocalDateTime.now()).toDays();
        if (ageDays < 0 || ageDays >= NEW_BAND_BOOST_DAYS) {
            return 0;
        }
        double decay = 1.0 - ((double) ageDays / NEW_BAND_BOOST_DAYS);
        return decay * NEW_BAND_BOOST_WEIGHT * SCORE_SCALE;
    }

    // 후보 밴드별 노출 피로도 감점 배율(0~1)을 계산한다. 클릭(관심)은 피로도를 영구 면제하는 게 아니라 리셋 시점으로만 쓴다.
    private Map<Long, Double> computeFatiguePenalties(Long userId, List<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(EXPOSURE_FATIGUE_LOOKBACK_DAYS);
        List<Object[]> exposureRows = bandRecommendationLogRepository
                .findRecentVisibleExposures(userId, candidateIds, EXPOSURE_VISIBLE_POSITION_LIMIT, since);
        if (exposureRows.isEmpty()) {
            return Map.of();
        }

        Map<Long, LocalDateTime> lastInteractedAtByBandId = bandInteractionRepository
                .findLastInteractedAtByUserIdAndBandIdIn(userId, candidateIds).stream()
                .filter(row -> row[1] != null)
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        Map<Long, Set<LocalDate>> exposureDaysByBandId = exposureRows.stream()
                .filter(row -> {
                    LocalDateTime lastInteractedAt = lastInteractedAtByBandId.get((Long) row[0]);
                    return lastInteractedAt == null || ((LocalDateTime) row[1]).isAfter(lastInteractedAt);
                })
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> ((LocalDateTime) row[1]).toLocalDate(), Collectors.toSet())));

        LocalDate today = LocalDate.now();
        Map<Long, Double> penaltyByBandId = new HashMap<>();
        exposureDaysByBandId.forEach((bandId, exposureDays) -> {
            double effectiveImpressions = exposureDays.stream()
                    .mapToDouble(day -> Math.exp(-LN2 / EXPOSURE_HALF_LIFE_DAYS * ChronoUnit.DAYS.between(day, today)))
                    .sum();
            double excess = Math.max(0, effectiveImpressions - EXPOSURE_GRACE);
            if (excess <= 0) {
                return;
            }

            double penalty = Math.exp(-EXPOSURE_PENALTY_ALPHA * excess);
            penaltyByBandId.put(bandId, Math.max(penalty, EXPOSURE_PENALTY_FLOOR));
        });

        return penaltyByBandId;
    }

    // 장르/지역 선호, 팔로우, 클릭 이력이 전부 없어 후보군이 빈 유저를 위한 폴백 후보 조회.
    // 팔로워 상위 밴드를 우선 채우고, 풀이 덜 차면 최근 생성 밴드로 보충한다.
    private ColdStartFallback collectColdStartFallback(Set<Long> excludedBandIds) {
        PageRequest pageable = PageRequest.of(0, COLD_START_FALLBACK_POOL_SIZE);

        Map<Long, Band> fallbackBands = new LinkedHashMap<>();
        Set<Long> popularBandIds = new HashSet<>();
        List<Long> topFollowedBandIds = followRepository.findTopFollowedBandIds(pageable);
        if (!topFollowedBandIds.isEmpty()) {
            bandRepository.findAllByIdInAndStatus(topFollowedBandIds, BandStatus.ACCEPTED).forEach(band -> {
                fallbackBands.put(band.getId(), band);
                popularBandIds.add(band.getId());
            });
        }

        Set<Long> newBandIds = new HashSet<>();
        if (fallbackBands.size() < COLD_START_FALLBACK_POOL_SIZE) {
            bandRepository.findAllByOrderByCreatedAtDesc(pageable).forEach(band -> {
                if (fallbackBands.putIfAbsent(band.getId(), band) == null) {
                    newBandIds.add(band.getId());
                }
            });
        }

        excludedBandIds.forEach(bandId -> {
            fallbackBands.remove(bandId);
            popularBandIds.remove(bandId);
            newBandIds.remove(bandId);
        });

        return new ColdStartFallback(new ArrayList<>(fallbackBands.values()), popularBandIds, newBandIds);
    }

    // 팔로우/클릭 시드 중 어느 쪽에서 온 유사도 행인지에 따른 가중치
    private double seedWeight(BandSimilarity bandSimilarity, Set<Long> followedBandIdSet) {
        return followedBandIdSet.contains(bandSimilarity.getBand().getId()) ? FOLLOW_SEED_WEIGHT : CLICK_SEED_WEIGHT;
    }

    // 동점 기여 요인 우선순위 : 유사도 > 장르 > 지역 > 최근 활동. 유사도가 1등이면 팔로우/클릭 중 어느 시드에서 왔는지로 문구를 나눈다.
    // 넷 다 0이면(콜드스타트 폴백 후보) 폴백 출처(신규 > 인기)로 문구를 대신 채운다.
    private String determineReason(
            double similarityContribution,
            double genreContribution,
            double regionContribution,
            double activityContribution,
            boolean similarityFromFollow,
            Long bandId,
            ColdStartFallback coldStartFallback
    ) {
        double max = Math.max(
                similarityContribution,
                Math.max(genreContribution, Math.max(regionContribution, activityContribution)));
        if (max <= 0) {
            if (coldStartFallback.newBandIds().contains(bandId)) {
                return REASON_NEW_BAND;
            }
            if (coldStartFallback.popularBandIds().contains(bandId)) {
                return REASON_POPULAR;
            }
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

    private record ColdStartFallback(List<Band> bands, Set<Long> popularBandIds, Set<Long> newBandIds) {
        private static final ColdStartFallback EMPTY = new ColdStartFallback(List.of(), Set.of(), Set.of());
    }
}
