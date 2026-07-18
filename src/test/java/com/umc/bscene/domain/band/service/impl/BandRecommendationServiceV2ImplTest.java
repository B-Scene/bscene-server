package com.umc.bscene.domain.band.service.impl;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private BandSimilarityRepository bandSimilarityRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BandRecommendationServiceV2Impl service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new BandRecommendationServiceV2Impl(
                bandRepository, followRepository, userRepository,
                userGenresRepository, userRegionsRepository,
                postRepository, bandSimilarityRepository, eventPublisher
        );
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
    }

    private Band band(Long id, Genre genre, Region region) {
        return Band.builder().id(id).name("band-" + id).genre(genre).region(region).build();
    }

    @Test
    void similarityBonusIsZeroWhenUserFollowsNoBand() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band candidate = band(10L, Genre.ROCK, Region.SEOUL);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(candidate));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(3.0, response.bands().get(0).score());
        verify(bandSimilarityRepository, never()).findByBandIdIn(anyList());
    }

    @Test
    void similarityAverageIsCappedBeforeNormalizing() {
        List<Long> followedBandIds = List.of(100L, 200L, 300L);
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(followedBandIds);
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        List<BandSimilarity> similarities = List.of(
                BandSimilarity.builder().band(band(100L, Genre.ROCK, Region.SEOUL)).similarBand(similarBand).score(15.0).build(),
                BandSimilarity.builder().band(band(200L, Genre.ROCK, Region.SEOUL)).similarBand(similarBand).score(15.0).build(),
                BandSimilarity.builder().band(band(300L, Genre.ROCK, Region.SEOUL)).similarBand(similarBand).score(15.0).build()
        );
        when(bandSimilarityRepository.findByBandIdIn(followedBandIds)).thenReturn(similarities);
        when(bandRepository.findAllById(any())).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        // raw sum 45.0 / followedCount 3 = avg 15.0 -> capped at 10.0 -> normalized 1.0 -> IMPLICIT_WEIGHT(0.4) * SCORE_SCALE(10) = 4.0
        assertEquals(4.0, response.bands().get(0).score(), 1e-9);
    }

    @Test
    void similarityScoreIsSameAcrossDifferentFollowedCountsGivenSameAverage() {
        // 팔로우 3개짜리 유저와 비교해, 팔로우 1개인데 유사도가 같은 유저도 동일한 평균(8.0)이면 동일 점수를 받아야 한다.
        // (기존 구현은 팔로우 개수만큼 유사도를 단순 합산해서, 팔로우가 많을수록 유리한 편향이 있었다.)
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(100L));
        when(userGenresRepository.findAllByUser(any(User.class))).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band similarBand = band(50L, Genre.JAZZ, Region.BUSAN);
        when(bandSimilarityRepository.findByBandIdIn(List.of(100L))).thenReturn(List.of(
                BandSimilarity.builder().band(band(100L, Genre.ROCK, Region.SEOUL)).similarBand(similarBand).score(8.0).build()
        ));
        when(bandRepository.findAllById(any())).thenReturn(List.of(similarBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        // avg 8.0 -> normalized 0.8 -> IMPLICIT_WEIGHT(0.4) * SCORE_SCALE(10) = 3.2
        assertEquals(3.2, response.bands().get(0).score(), 1e-9);
    }

    @Test
    void alreadyFollowedBandIsExcludedFromResult() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        Band followedBand = band(10L, Genre.ROCK, Region.SEOUL);
        Band otherBand = band(20L, Genre.ROCK, Region.BUSAN);
        when(bandRepository.findByGenreIn(any())).thenReturn(List.of(followedBand, otherBand));
        when(bandSimilarityRepository.findByBandIdIn(List.of(10L))).thenReturn(List.of());
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(20L, response.bands().get(0).bandId());
    }

    @Test
    void reasonPicksHighestPriorityFactorWhenGenreRegionAndActivityAllMatch() {
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(userGenresRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserGenres.builder().genre(Genre.ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class)))
                .thenReturn(List.of(UserRegions.builder().region(Region.SEOUL).build()));

        Band candidate = band(1L, Genre.ROCK, Region.SEOUL);
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

        Band older = band(1L, Genre.ROCK, Region.SEOUL);
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
                .thenReturn(List.of(UserGenres.builder().genre(Genre.ROCK).build()));
        when(userRegionsRepository.findAllByUser(any(User.class))).thenReturn(List.of());

        List<Band> manyCandidates = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(i -> band((long) i, Genre.ROCK, Region.SEOUL))
                .toList();
        when(bandRepository.findByGenreIn(any())).thenReturn(manyCandidates);
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertTrue(response.bands().size() <= 10);
        assertEquals(10, response.bands().size());
    }
}
