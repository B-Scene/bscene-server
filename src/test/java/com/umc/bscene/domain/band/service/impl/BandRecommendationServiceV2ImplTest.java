package com.umc.bscene.domain.band.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.recommendation.entity.BandInteraction;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import com.umc.bscene.domain.recommendation.repository.BandRecommendationLogRepository;
import com.umc.bscene.domain.recommendation.repository.BandSimilarityRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandRecommendationServiceV2ImplTest {

    @Mock
    private BandRepository bandRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserGenresRepository userGenresRepository;
    @Mock
    private UserRegionsRepository userRegionsRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private BandSimilarityRepository bandSimilarityRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BandInteractionRepository bandInteractionRepository;
    @Mock
    private BandRecommendationLogRepository bandRecommendationLogRepository;

    private BandRecommendationServiceV2Impl service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new BandRecommendationServiceV2Impl(
                bandRepository, followRepository, userRepository,
                userGenresRepository, userRegionsRepository,
                postRepository, performanceRepository, bandSimilarityRepository, eventPublisher,
                bandInteractionRepository, bandRecommendationLogRepository
        );
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
    }

    private Band band(Long id, Genre genre, Region region) {
        return Band.builder().id(id).name("band-" + id).genre(genre).region(region).build();
    }

    @Test
    void unknownPreferredRegionIsIgnoredAndNeverQueried() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserRegions.builder().region(Region.UNKNOWN).build()));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(0, response.bands().size());
        verify(bandRepository, never()).findByRegionIn(any());
    }

    @Test
    void similarityBonusIsZeroWhenUserFollowsNoBand() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band candidate = band(10L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(candidate));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(3.0, response.bands().get(0).score());
        verify(bandSimilarityRepository, never()).findByBandIdIn(anyList());
    }

    @Test
    void withDummyDefaultsToTrueAndIncludesDummyBands() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band dummyBand = band(474L, Genre.HARD_ROCK, Region.SEOUL);
        Band realBand = band(475L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(dummyBand, realBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        // withDummy 생략 -> 기본값 true, 더미 밴드도 그대로 포함
        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
    }

    @Test
    void withDummyFalseExcludesDummyBands() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band dummyBand = band(474L, Genre.HARD_ROCK, Region.SEOUL);
        Band realBand = band(475L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(dummyBand, realBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null, false);

        assertEquals(1, response.bands().size());
        assertEquals(475L, response.bands().get(0).bandId());
    }

    @Test
    void similarityAverageIsCappedBeforeNormalizing() {
        List<Long> followedBandIds = List.of(100L, 200L, 300L);
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(followedBandIds);
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        // score는 ml-server가 코사인 유사도로 계산해 저장하는 실제 스케일(threshold 0.55 ~ 최대 1.0)을 반영한 값.
        List<BandSimilarity> similarities = List.of(
                BandSimilarity.builder().band(band(100L, Genre.HARD_ROCK, Region.SEOUL)).similarBand(similarBand).score(0.9).build(),
                BandSimilarity.builder().band(band(200L, Genre.HARD_ROCK, Region.SEOUL)).similarBand(similarBand).score(0.9).build(),
                BandSimilarity.builder().band(band(300L, Genre.HARD_ROCK, Region.SEOUL)).similarBand(similarBand).score(0.9).build()
        );
        when(bandSimilarityRepository.findByBandIdIn(followedBandIds)).thenReturn(similarities);
        when(bandRepository.findAllByIdInAndStatus(any(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        // raw sum 2.7 / followedCount 3 = avg 0.9 -> capped at 0.8 -> normalized 1.0 -> IMPLICIT_WEIGHT(0.4) * SCORE_SCALE(10) = 4.0
        assertEquals(4.0, response.bands().get(0).score(), 1e-9);
    }

    @Test
    void similarityScoreIsSameAcrossDifferentFollowedCountsGivenSameAverage() {
        // 팔로우 3개짜리 유저와 비교해, 팔로우 1개인데 유사도가 같은 유저도 동일한 평균이면 동일 점수를 받아야 한다.
        // (기존 구현은 팔로우 개수만큼 유사도를 단순 합산해서, 팔로우가 많을수록 유리한 편향이 있었다.)
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(100L));
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        when(bandSimilarityRepository.findByBandIdIn(List.of(100L))).thenReturn(List.of(
                BandSimilarity.builder().band(band(100L, Genre.HARD_ROCK, Region.SEOUL)).similarBand(similarBand).score(0.4).build()
        ));
        when(bandRepository.findAllByIdInAndStatus(any(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2.0, response.bands().get(0).score(), 1e-9);
    }

    @Test
    void frequentlyClickedBandContributesToSimilaritySeeds() {
        // 팔로우는 안 했지만 자주 클릭한(관심 있는) 밴드도 유사도 조회 시드로 쓰여야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band clickedBand = band(100L, Genre.HARD_ROCK, Region.SEOUL);
        BandInteraction interaction = BandInteraction.builder()
                .user(User.builder().id(USER_ID).build())
                .band(clickedBand)
                .clickCount(5)
                .build();
        when(bandInteractionRepository.findByUser_IdOrderByClickCountDesc(eq(USER_ID), any()))
                .thenReturn(List.of(interaction));

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        when(bandSimilarityRepository.findByBandIdIn(List.of(100L))).thenReturn(List.of(
                BandSimilarity.builder().band(clickedBand).similarBand(similarBand).score(0.8).build()
        ));
        when(bandRepository.findAllByIdInAndStatus(any(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(50L, response.bands().get(0).bandId());
        // 클릭 전용 시드라 raw 0.8 * CLICK_SEED_WEIGHT(0.5) = 0.4 -> 시드 1개로 나눠 avg 0.4
        // -> normalized 0.4/0.8=0.5 -> IMPLICIT_WEIGHT(0.4) * SCORE_SCALE(10) * 0.5 = 2.0
        assertEquals(2.0, response.bands().get(0).score(), 1e-9);
        assertEquals("관심 있게 본 밴드와 유사한 스타일", response.bands().get(0).reason());
    }

    @Test
    void unrelatedClickedBandsDoNotDiluteFollowBasedSimilarity() {
        // 후보와 무관한 클릭 시드가 여러 개 있어도, 실제 매칭된 팔로우 시드 기준 평균은 그대로여야 한다
        // (분모가 유저의 전체 시드 개수라면 클릭이 많을수록 이 평균이 부당하게 희석된다).
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(100L));
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        List<BandInteraction> unrelatedClicks = List.of(200L, 201L, 202L, 203L, 204L).stream()
                .map(id -> BandInteraction.builder()
                        .user(User.builder().id(USER_ID).build())
                        .band(band(id, Genre.JAZZ, Region.BUSAN))
                        .clickCount(3)
                        .build())
                .toList();
        when(bandInteractionRepository.findByUser_IdOrderByClickCountDesc(eq(USER_ID), any()))
                .thenReturn(unrelatedClicks);

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        // 클릭한 200~204번 밴드는 이 후보와 유사도 행 자체가 없다 (findByBandIdIn 결과에 안 잡힘) - 팔로우 100번만 매칭.
        when(bandSimilarityRepository.findByBandIdIn(anyList())).thenReturn(List.of(
                BandSimilarity.builder().band(band(100L, Genre.HARD_ROCK, Region.SEOUL)).similarBand(similarBand).score(0.6).build()
        ));
        when(bandRepository.findAllByIdInAndStatus(any(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        // avg 0.6 (매칭된 시드는 팔로우 1개뿐 - 클릭 5개는 분모에 안 들어감) -> normalized 0.6/0.8=0.75
        // -> IMPLICIT_WEIGHT(0.4) * SCORE_SCALE(10) * 0.75 = 3.0
        assertEquals(1, response.bands().size());
        assertEquals(3.0, response.bands().get(0).score(), 1e-9);
        assertEquals("팔로우한 밴드와 유사한 스타일", response.bands().get(0).reason());
    }

    @Test
    void alreadyFollowedBandIsExcludedFromResult() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band followedBand = band(10L, Genre.HARD_ROCK, Region.SEOUL);
        Band otherBand = band(20L, Genre.HARD_ROCK, Region.BUSAN);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(followedBand, otherBand));
        when(bandSimilarityRepository.findByBandIdIn(List.of(10L))).thenReturn(List.of());
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(20L, response.bands().get(0).bandId());
    }

    @Test
    void recentPerformanceCountsAsActivityEvenWithoutRecentPost() {
        // 최근 포스트가 없어도 최근/예정 공연이 있으면 활동 점수를 받아야 한다 (V1엔 있었는데 V2엔 빠져있던 신호).
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band candidate = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(candidate));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), eq(PerformanceStatus.ACTIVE), any()))
                .thenReturn(List.of(1L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        BandRecommendItem item = response.bands().get(0);
        assertEquals(4.0, item.score(), 1e-9); // genre(3) + activity(1)
    }

    @Test
    void reasonPicksHighestPriorityFactorWhenGenreRegionAndActivityAllMatch() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserRegions.builder().region(Region.SEOUL).build()));

        Band candidate = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(candidate));
        when(bandRepository.findByRegionIn(any())).thenReturn(List.of(candidate));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of(1L));
        when(postRepository.findLatestActivityAtByBandIds(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, LocalDateTime.now()}));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        BandRecommendItem item = response.bands().get(0);
        assertEquals(6.0, item.score()); // genre(3) + region(2) + activity(1)
        assertEquals("선호 장르 일치", item.reason());
    }

    @Test
    void tiedScoresAreOrderedByMostRecentActivityFirst() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserRegions.builder().region(Region.SEOUL).build()));

        Band older = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        Band newer = band(2L, Genre.JAZZ, Region.SEOUL);
        when(bandRepository.findByRegionIn(any())).thenReturn(List.of(older, newer));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, LocalDateTime.of(2026, 1, 1, 0, 0)},
                new Object[]{2L, LocalDateTime.of(2026, 6, 1, 0, 0)}
        ));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        assertEquals(2L, response.bands().get(0).bandId());
        assertEquals(1L, response.bands().get(1).bandId());
    }

    @Test
    void resultNeverExceedsTenBands() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        List<Band> manyCandidates = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> band((long) i, Genre.HARD_ROCK, Region.SEOUL))
                .toList();
        when(bandRepository.findByGenreIn(any())).thenReturn(manyCandidates);
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertTrue(response.bands().size() <= 10);
        assertEquals(10, response.bands().size());
    }

    @Test
    void cursorPaginatesThroughFullSortedResultSet() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        List<Band> candidates = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> band((long) i, Genre.HARD_ROCK, Region.SEOUL))
                .toList();
        when(bandRepository.findByGenreIn(any())).thenReturn(candidates);
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse firstPage = service.getRecommendedBands(USER_ID, null, 5);
        assertEquals(5, firstPage.bands().size());
        assertTrue(firstPage.hasNext());
        assertEquals(5L, firstPage.nextCursor());
        assertEquals(1L, firstPage.bands().get(0).bandId());

        BandRecommendResponse secondPage = service.getRecommendedBands(USER_ID, firstPage.nextCursor(), 5);
        assertEquals(5, secondPage.bands().size());
        assertTrue(secondPage.hasNext());
        assertEquals(10L, secondPage.nextCursor());
        assertEquals(6L, secondPage.bands().get(0).bandId());

        BandRecommendResponse thirdPage = service.getRecommendedBands(USER_ID, secondPage.nextCursor(), 5);
        assertEquals(5, thirdPage.bands().size());
        assertFalse(thirdPage.hasNext());
        assertNull(thirdPage.nextCursor());
        assertEquals(11L, thirdPage.bands().get(0).bandId());
    }

    @Test
    void coldStartFallbackReturnsPopularBandWhenUserHasNoSignalsAtAll() {
        // 장르/지역 선호, 팔로우, 클릭 이력이 전혀 없는 신규 유저 - 후보군이 비므로 인기 밴드 폴백이 채워야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band popularBand = band(100L, Genre.HARD_ROCK, Region.SEOUL);
        when(followRepository.findTopFollowedBandIds(any(Pageable.class))).thenReturn(List.of(100L));
        when(bandRepository.findAllByIdInAndStatus(anyList(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(popularBand));
        when(bandRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(100L, response.bands().get(0).bandId());
        assertEquals("요즘 인기있는 밴드", response.bands().get(0).reason());
    }

    @Test
    void coldStartFallbackUsesRecentlyCreatedBandsWhenNoBandHasFollowersYet() {
        // 서비스 초창기라 팔로우 데이터 자체가 없는 경우 - 최근 생성된 밴드로 보충되어야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band newBand = band(200L, Genre.JAZZ, Region.BUSAN);
        when(followRepository.findTopFollowedBandIds(any(Pageable.class))).thenReturn(List.of());
        when(bandRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(newBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(200L, response.bands().get(0).bandId());
        assertEquals("새로 등록된 밴드", response.bands().get(0).reason());
    }

    @Test
    void recentlyCreatedBandOutscoresOlderBandWithIdenticalOtherSignals() {
        // 팔로우/활동 신호가 동일해도 최근 생성된 밴드는 신생 밴드 부스트만큼 더 높은 점수를 받아야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band newBand = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        Band oldBand = band(2L, Genre.HARD_ROCK, Region.SEOUL);
        ReflectionTestUtils.setField(newBand, "createdAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(oldBand, "createdAt", LocalDateTime.now().minusDays(60));

        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(newBand, oldBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        assertEquals(1L, response.bands().get(0).bandId());
        assertEquals(2L, response.bands().get(1).bandId());
        assertTrue(response.bands().get(0).score() > response.bands().get(1).score());
    }

    @Test
    void coldStartFallbackExcludesBandsTheUserAlreadyFollows() {
        // 유일한 신호가 팔로우뿐이고 아직 유사도 데이터가 없어 후보군이 비는 경우 - 폴백이 채워주되,
        // 팔로우한 밴드가 마침 인기 폴백 풀에도 걸리더라도 이미 팔로우 중인 밴드는 다시 추천되면 안 된다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(100L));
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(bandSimilarityRepository.findByBandIdIn(anyList())).thenReturn(List.of());

        Band followedBand = band(100L, Genre.HARD_ROCK, Region.SEOUL);
        Band otherPopularBand = band(300L, Genre.JAZZ, Region.BUSAN);
        when(followRepository.findTopFollowedBandIds(any(Pageable.class))).thenReturn(List.of(100L, 300L));
        when(bandRepository.findAllByIdInAndStatus(anyList(), eq(BandStatus.ACCEPTED))).thenReturn(List.of(followedBand, otherPopularBand));
        when(bandRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(300L, response.bands().get(0).bandId());
        assertEquals("요즘 인기있는 밴드", response.bands().get(0).reason());
    }

    @Test
    void repeatedlyExposedBandWithoutClickRanksBelowFreshMatch() {
        // 최근 3일 연속 노출됐는데 클릭이 없었던 밴드는 제외되지 않고 순위만 밀려야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band fatiguedBand = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        Band freshBand = band(2L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(fatiguedBand, freshBand));

        LocalDateTime now = LocalDateTime.now();
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, now},
                        new Object[]{1L, now.minusDays(1)},
                        new Object[]{1L, now.minusDays(2)}
                ));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        assertEquals(2L, response.bands().get(0).bandId());
        assertEquals(1L, response.bands().get(1).bandId());
        assertTrue(response.bands().get(0).score() > response.bands().get(1).score());
    }

    @Test
    void onlyMatchStaysRankedFirstDespiteHeavyFatigue() {
        // 후보가 이 밴드 하나뿐이면, 아무리 피로도가 쌓여도(FLOOR) 제외되지 않고 그대로 1등으로 남아야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band onlyMatch = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(onlyMatch));

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> heavyExposures = java.util.stream.IntStream.range(0, 20)
                .mapToObj(daysAgo -> new Object[]{1L, now.minusDays(daysAgo)})
                .toList();
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(heavyExposures);
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        // genre(3)만 매치 -> base score 3.0. FLOOR(0.25) 적용 시 최저 3.0 * 0.25 = 0.75까지만 깎여야 한다.
        assertEquals(1, response.bands().size());
        assertEquals(1L, response.bands().get(0).bandId());
        assertEquals(0.75, response.bands().get(0).score(), 0.05);
    }

    @Test
    void exposuresBeforeClickAreIgnoredSoNoFatiguePenaltyApplies() {
        // 클릭이 모든 노출보다 나중이면 - 그 노출들은 리셋되어 다 무시되고 페널티가 아예 없어야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band clickedBand = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        Band otherBand = band(2L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(clickedBand, otherBand));

        LocalDateTime now = LocalDateTime.now();
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, now},
                        new Object[]{1L, now.minusDays(1)},
                        new Object[]{1L, now.minusDays(2)}
                ));
        when(bandInteractionRepository.findLastInteractedAtByUserIdAndBandIdIn(eq(USER_ID), anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, now.plusSeconds(1)}));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        // 둘 다 무페널티라 genre(3)만 매치한 동일 base score(3.0)를 받아야 한다.
        assertEquals(3.0, response.bands().get(0).score(), 1e-9);
        assertEquals(3.0, response.bands().get(1).score(), 1e-9);
    }

    @Test
    void exposuresAfterClickStillAccumulateFatigueDespiteEarlierClick() {
        // 클릭은 "영구 면제권"이 아니라 리셋 시점일 뿐이다 - 클릭 이후에 또 반복 노출되고
        // 그 사이 재클릭이 없었다면, 클릭 이전 이력과 무관하게 다시 피로도가 쌓여야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band clickedThenIgnoredAgainBand = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(clickedThenIgnoredAgainBand));

        LocalDateTime now = LocalDateTime.now();
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(List.<Object[]>of(
                        // 클릭(10일 전) 이전 노출 - 리셋되어 무시돼야 함
                        new Object[]{1L, now.minusDays(20)},
                        new Object[]{1L, now.minusDays(19)},
                        new Object[]{1L, now.minusDays(18)},
                        // 클릭 이후 노출 - 재클릭 없이 반복됐으므로 다시 피로도에 반영돼야 함
                        new Object[]{1L, now},
                        new Object[]{1L, now.minusDays(1)},
                        new Object[]{1L, now.minusDays(2)}
                ));
        when(bandInteractionRepository.findLastInteractedAtByUserIdAndBandIdIn(eq(USER_ID), anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, now.minusDays(10)}));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        // 클릭 이전 3회는 리셋으로 버려지고, 클릭 이후 3회(오늘/어제/그제)만 계산돼야 한다.
        // genre(3) base -> effImp≈2.42, excess≈0.42, penalty≈0.81 -> score≈2.43
        assertEquals(1, response.bands().size());
        assertTrue(response.bands().get(0).score() < 3.0);
        assertEquals(2.43, response.bands().get(0).score(), 0.05);
    }

    @Test
    void fatiguePenaltyRecoversAsExposuresAgeOut() {
        // 노출 횟수는 같아도(3회), 오래전에 몰려있던 노출은 감쇠로 회복되어 무페널티에 가까워지고
        // 최근에 몰린 노출은 여전히 페널티를 받아야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band recentlyFatiguedBand = band(10L, Genre.HARD_ROCK, Region.SEOUL);
        Band recoveredBand = band(20L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(recentlyFatiguedBand, recoveredBand));

        LocalDateTime now = LocalDateTime.now();
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{10L, now},
                        new Object[]{10L, now.minusDays(1)},
                        new Object[]{10L, now.minusDays(2)},
                        new Object[]{20L, now.minusDays(15)},
                        new Object[]{20L, now.minusDays(16)},
                        new Object[]{20L, now.minusDays(17)}
                ));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        BandRecommendItem recovered = response.bands().stream()
                .filter(item -> item.bandId().equals(20L)).findFirst().orElseThrow();
        BandRecommendItem stillFatigued = response.bands().stream()
                .filter(item -> item.bandId().equals(10L)).findFirst().orElseThrow();

        assertEquals(3.0, recovered.score(), 0.05); // 오래된 노출 -> 감쇠로 사실상 무페널티
        assertTrue(stillFatigued.score() < recovered.score()); // 최근 노출 -> 아직 페널티 남아있음
    }

    @Test
    void fullyFatiguedBandFullyRecoversFourDaysAfterExposuresStop() {
        // 20일 연속 노출로 FLOOR까지 눌린 상태(effImp≈4.8)에서 노출이 완전히 끊겼을 때,
        // 감쇠만으로 3일 뒤엔 아직 페널티가 남아있고(excess>0) 4일 뒤엔 완전히 회복(excess<=0)돼야 한다.
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.HARD_ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band stillFatiguedBand = band(3L, Genre.HARD_ROCK, Region.SEOUL); // 마지막 노출 3일 전
        Band recoveredBand = band(4L, Genre.HARD_ROCK, Region.SEOUL); // 마지막 노출 4일 전
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(stillFatiguedBand, recoveredBand));

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> exposures = new java.util.ArrayList<>();
        for (int daysAgo = 0; daysAgo < 20; daysAgo++) {
            exposures.add(new Object[]{3L, now.minusDays(daysAgo + 3)});
            exposures.add(new Object[]{4L, now.minusDays(daysAgo + 4)});
        }
        when(bandRecommendationLogRepository.findRecentVisibleExposures(eq(USER_ID), anyList(), anyInt(), any()))
                .thenReturn(exposures);
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList())).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        BandRecommendItem stillFatigued = response.bands().stream()
                .filter(item -> item.bandId().equals(3L)).findFirst().orElseThrow();
        BandRecommendItem recovered = response.bands().stream()
                .filter(item -> item.bandId().equals(4L)).findFirst().orElseThrow();

        // genre(3)만 매치한 base score는 3.0. 3일 뒤엔 여전히 페널티(약 0.82배)가 남아있어야 한다.
        assertTrue(stillFatigued.score() < 3.0);
        assertEquals(2.46, stillFatigued.score(), 0.05);
        // 4일 뒤엔 excess<=0으로 완전히 회복돼 base score 그대로여야 한다.
        assertEquals(3.0, recovered.score(), 1e-6);
    }
}
