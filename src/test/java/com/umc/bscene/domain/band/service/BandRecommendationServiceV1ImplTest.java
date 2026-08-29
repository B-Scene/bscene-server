package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.response.BandRecommendItem;
import com.umc.bscene.domain.band.dto.response.BandRecommendResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.service.impl.BandRecommendationServiceV1Impl;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandRecommendationServiceV1ImplTest {

    @Mock
    private BandRepository bandRepository;
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

    private BandRecommendationServiceV1Impl service;

    private static final Long USER_ID = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        service = new BandRecommendationServiceV1Impl(
                bandRepository, userRepository, userGenresRepository, userRegionsRepository,
                postRepository, performanceRepository
        );
        user = User.builder().id(USER_ID).build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        lenient().when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of());
        lenient().when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(PerformanceStatus.class), any()))
                .thenReturn(List.of());
    }

    private Band band(Long id, Genre genre, Region region) {
        return Band.builder().id(id).name("밴드" + id).genre(genre).region(region).build();
    }

    private void preferredGenre(Genre genre) {
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(UserGenres.builder().genre(genre).build()));
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of());
    }

    private void preferredGenreAndRegion(Genre genre, Region region) {
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(UserGenres.builder().genre(genre).build()));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(UserRegions.builder().region(region).build()));
    }

    @Test
    void 최소점수_미만인_밴드는_추천에서_제외된다() {
        preferredGenreAndRegion(Genre.HARD_ROCK, Region.SEOUL);
        Band matched = band(1L, Genre.HARD_ROCK, Region.BUSAN); // genre만 일치 : 3점
        Band unmatched = band(2L, Genre.INDIE, Region.SEOUL); // region만 일치 : 2점 (기준 미달)
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(matched, unmatched));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(1L, response.bands().get(0).bandId());
    }

    @Test
    void 장르와_지역이_모두_일치하면_더_높은_점수로_상위에_정렬된다() {
        preferredGenreAndRegion(Genre.HARD_ROCK, Region.SEOUL);
        Band genreOnly = band(1L, Genre.HARD_ROCK, Region.BUSAN); // 3점
        Band genreAndRegion = band(2L, Genre.HARD_ROCK, Region.SEOUL); // 5점
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(genreOnly, genreAndRegion));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        List<BandRecommendItem> bands = response.bands();
        assertEquals(2, bands.size());
        assertEquals(2L, bands.get(0).bandId());
        assertEquals(1L, bands.get(1).bandId());
    }

    @Test
    void 최근_포스트와_공연_활동_점수가_합산되어_기준을_넘으면_추천된다() {
        preferredGenre(Genre.HARD_ROCK); // HARD_ROCK 선호, region 없음
        Band activeBand = band(1L, Genre.INDIE, Region.BUSAN); // 장르/지역 불일치, 활동만으로 3점
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(activeBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of(1L));
        when(performanceRepository.findBandIdsWithRecentPerformance(anyList(), any(PerformanceStatus.class), any()))
                .thenReturn(List.of(1L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(1, response.bands().size());
        assertEquals(1L, response.bands().get(0).bandId());
    }

    @Test
    void 최근_포스트_활동만으로는_기준점수에_미달해_제외된다() {
        preferredGenre(Genre.HARD_ROCK);
        Band postOnlyBand = band(1L, Genre.INDIE, Region.BUSAN); // 장르/지역 불일치, 포스트만 2점
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(postOnlyBand));
        when(postRepository.findBandIdsWithRecentPost(anyList(), any())).thenReturn(List.of(1L));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertTrue(response.bands().isEmpty());
    }

    @Test
    void 커서와_사이즈로_페이지네이션된다() {
        preferredGenre(Genre.HARD_ROCK);
        // 모두 장르만 일치(3점 동점) -> tie-break로 bandId 내림차순 정렬 (3, 2, 1)
        Band b1 = band(1L, Genre.HARD_ROCK, Region.BUSAN);
        Band b2 = band(2L, Genre.HARD_ROCK, Region.BUSAN);
        Band b3 = band(3L, Genre.HARD_ROCK, Region.BUSAN);
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(b1, b2, b3));

        BandRecommendResponse firstPage = service.getRecommendedBands(USER_ID, null, 2);

        assertEquals(2, firstPage.bands().size());
        assertEquals(List.of(3L, 2L), firstPage.bands().stream().map(BandRecommendItem::bandId).toList());
        assertTrue(firstPage.hasNext());
        assertEquals(2L, firstPage.nextCursor());

        BandRecommendResponse secondPage = service.getRecommendedBands(USER_ID, firstPage.nextCursor(), 2);

        assertEquals(1, secondPage.bands().size());
        assertEquals(1L, secondPage.bands().get(0).bandId());
        assertFalse(secondPage.hasNext());
        assertEquals(null, secondPage.nextCursor());
    }

    @Test
    void 선호_장르와_지역이_없으면_활동_점수만으로는_추천되지_않는다() {
        when(userGenresRepository.findAllByUser(user)).thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of());
        Band band = band(1L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(band));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertTrue(response.bands().isEmpty());
    }

    @Test
    void withDummy가_false면_더미_밴드_id는_후보에서_제외된다() {
        preferredGenreAndRegion(Genre.HARD_ROCK, Region.SEOUL);
        Band dummyBand = band(474L, Genre.HARD_ROCK, Region.SEOUL); // 더미 경계(<=474)
        Band realBand = band(475L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(dummyBand, realBand));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null, false);

        assertEquals(1, response.bands().size());
        assertEquals(475L, response.bands().get(0).bandId());
    }

    @Test
    void withDummy_생략시_기본값true로_더미_밴드도_포함된다() {
        preferredGenreAndRegion(Genre.HARD_ROCK, Region.SEOUL);
        Band dummyBand = band(474L, Genre.HARD_ROCK, Region.SEOUL);
        Band realBand = band(475L, Genre.HARD_ROCK, Region.SEOUL);
        when(bandRepository.findAllByStatus(BandStatus.ACCEPTED)).thenReturn(List.of(dummyBand, realBand));

        BandRecommendResponse response = service.getRecommendedBands(USER_ID, null, null);

        assertEquals(2, response.bands().size());
    }
}
