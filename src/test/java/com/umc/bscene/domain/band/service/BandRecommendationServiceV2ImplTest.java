package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.impl.BandRecommendationServiceV2Impl;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.recommendation.entity.BandSimilarity;
import com.umc.bscene.domain.recommendation.event.BandRecommendationExposedEvent;
import com.umc.bscene.domain.recommendation.repository.BandInteractionRepository;
import com.umc.bscene.domain.recommendation.repository.BandSimilarityRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
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

    private BandRecommendationServiceV2Impl service;

    private static final Long USER_ID = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        service = new BandRecommendationServiceV2Impl(
                bandRepository, followRepository, userRepository, userGenresRepository, userRegionsRepository,
                postRepository, performanceRepository, bandSimilarityRepository, eventPublisher,
                bandInteractionRepository
        );
        user = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(userGenresRepository.findAllByUser(user)).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of());
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of());
        when(bandInteractionRepository.findByUser_IdOrderByClickCountDesc(any(), any(Pageable.class)))
                .thenReturn(List.of());
    }

    private Band band(Long id, Genre genre, Region region) {
        return Band.builder().id(id).name("밴드" + id).genre(genre).region(region).build();
    }

    private BandSimilarity similarity(Band band, Band similarBand, double score) {
        return BandSimilarity.builder().band(band).similarBand(similarBand).score(score).build();
    }

    private void preferGenre(Genre genre) {
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(UserGenres.builder().genre(genre).build()));
    }

    private void stubEmptyActivityAndPopularity(List<Long> candidateIds) {
        when(postRepository.findBandIdsWithRecentPost(eqIds(candidateIds), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(eqIds(candidateIds))).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(eqIds(candidateIds), any(), any()))
                .thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(eqIds(candidateIds))).thenReturn(List.of());
        when(followRepository.countRecentFollowersByBandIdIn(eqIds(candidateIds), any())).thenReturn(List.of());
    }

    private List<Long> eqIds(List<Long> ids) {
        return org.mockito.ArgumentMatchers.argThat(arg -> arg != null && Set.copyOf(arg).equals(Set.copyOf(ids)));
    }

    @Test
    void 선호_장르가_일치하는_밴드가_추천되고_이유는_장르일치이다() {
        preferGenre(Genre.HARD_ROCK);
        Band matched = band(10L, Genre.HARD_ROCK, Region.BUSAN);
        when(bandRepository.findByGenreIn(anyCollection())).thenReturn(List.of(matched));
        stubEmptyActivityAndPopularity(List.of(10L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        BandRecommendItem item = response.bands().get(0);
        assertEquals(10L, item.bandId());
        assertEquals("선호 장르 일치", item.reason());
        assertTrue(item.score() > 0);
    }

    @Test
    void 팔로우한_밴드는_장르가_일치해도_추천_후보에서_제외된다() {
        preferGenre(Genre.HARD_ROCK);
        Band followed = band(10L, Genre.HARD_ROCK, Region.BUSAN);
        Band other = band(20L, Genre.HARD_ROCK, Region.BUSAN);
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(bandRepository.findByGenreIn(anyCollection())).thenReturn(List.of(followed, other));
        when(bandSimilarityRepository.findByBandIdIn(anyList())).thenReturn(List.of());
        stubEmptyActivityAndPopularity(List.of(20L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(20L, response.bands().get(0).bandId());
    }

    @Test
    void 팔로우한_밴드와_유사한_밴드가_추천되고_이유는_팔로우_유사도이다() {
        Band followedBand = band(10L, Genre.HARD_ROCK, Region.SEOUL);
        Band similarBand = band(99L, Genre.INDIE, Region.BUSAN);
        when(followRepository.findBandIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(bandSimilarityRepository.findByBandIdIn(anyList()))
                .thenReturn(List.of(similarity(followedBand, similarBand, 0.9)));
        when(bandRepository.findAllById(anyCollection())).thenReturn(List.of(similarBand));
        stubEmptyActivityAndPopularity(List.of(99L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        BandRecommendItem item = response.bands().get(0);
        assertEquals(99L, item.bandId());
        assertEquals("팔로우한 밴드와 유사한 스타일", item.reason());
    }

    @Test
    void 팔로워수가_많은_밴드가_동일조건에서_더_높은_점수를_받는다() {
        preferGenre(Genre.HARD_ROCK);
        Band popular = band(1L, Genre.HARD_ROCK, Region.BUSAN);
        Band normal = band(2L, Genre.HARD_ROCK, Region.BUSAN);
        when(bandRepository.findByGenreIn(anyCollection())).thenReturn(List.of(popular, normal));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        when(postRepository.findLatestActivityAtByBandIds(anyList())).thenReturn(List.of());
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(), any())).thenReturn(List.of());
        when(followRepository.countFollowersByBandIds(anyList()))
                .thenReturn(List.of(new Object[]{1L, 100L}, new Object[]{2L, 1L}));
        when(followRepository.countRecentFollowersByBandIdIn(anyList(), any()))
                .thenReturn(List.of(new Object[]{1L, 20L}, new Object[]{2L, 0L}));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
        assertEquals(1L, response.bands().get(0).bandId());
        assertEquals(2L, response.bands().get(1).bandId());
        assertTrue(response.bands().get(0).score() > response.bands().get(1).score());
    }

    @Test
    void 커서와_사이즈로_페이지네이션되며_노출_이벤트가_순번과_함께_발행된다() {
        preferGenre(Genre.HARD_ROCK);
        Band b1 = band(1L, Genre.HARD_ROCK, Region.BUSAN);
        Band b2 = band(2L, Genre.HARD_ROCK, Region.BUSAN);
        Band b3 = band(3L, Genre.HARD_ROCK, Region.BUSAN);
        when(bandRepository.findByGenreIn(anyCollection())).thenReturn(List.of(b1, b2, b3));
        stubEmptyActivityAndPopularity(List.of(1L, 2L, 3L));

        BandRecommendResponse firstPage = service.getRecommendedBands(USER_ID, null, 2);

        assertEquals(2, firstPage.bands().size());
        assertTrue(firstPage.hasNext());
        assertEquals(2L, firstPage.nextCursor());

        ArgumentCaptor<BandRecommendationExposedEvent> captor =
                ArgumentCaptor.forClass(BandRecommendationExposedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        BandRecommendationExposedEvent event = captor.getValue();
        assertEquals(USER_ID, event.userId());
        assertEquals(2, event.exposures().size());
        assertEquals(1, event.exposures().get(0).position());
        assertEquals(2, event.exposures().get(1).position());

        BandRecommendResponse secondPage = service.getRecommendedBands(USER_ID, firstPage.nextCursor(), 2);

        assertEquals(1, secondPage.bands().size());
        assertFalse(secondPage.hasNext());
    }

    @Test
    void 선호_장르와_지역이_모두_없고_유사도_시드도_없으면_인기_밴드로_폴백된다() {
        // 온보딩상 장르/지역은 필수 선택이라 실제로는 드물지만, 그래도 신호가 하나도 없을 때
        // 빈 결과 대신 인기 밴드 폴백이 채워져야 한다 (콜드스타트 대응).
        Band popularBand = band(100L, Genre.HARD_ROCK, Region.SEOUL);
        when(followRepository.findTopFollowedBandIds(any(Pageable.class))).thenReturn(List.of(100L));
        when(bandRepository.findAllById(anyCollection())).thenReturn(List.of(popularBand));
        when(bandRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());
        stubEmptyActivityAndPopularity(List.of(100L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(100L, response.bands().get(0).bandId());
        assertEquals("요즘 인기있는 밴드", response.bands().get(0).reason());
        assertFalse(response.hasNext());
    }
}
