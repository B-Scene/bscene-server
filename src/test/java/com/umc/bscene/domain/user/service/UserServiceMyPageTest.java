package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.user.dto.request.MyInfoUpdateRequest;
import com.umc.bscene.domain.user.dto.response.MyInfoResponse;
import com.umc.bscene.domain.user.dto.response.mypage.FanMyPageResponse;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.exception.UserException;
import com.umc.bscene.domain.user.port.AuthPort;
import com.umc.bscene.domain.user.port.BandPort;
import com.umc.bscene.domain.user.port.FollowPort;
import com.umc.bscene.domain.user.port.NotifyPort;
import com.umc.bscene.domain.user.port.PerformancePort;
import com.umc.bscene.domain.user.port.SessionPort;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.domain.user.response.code.UserErrorCode;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 팬 마이페이지 계열 (getFanMyPage / 참여 기록·관심 공연 연도 필터 / 팔로우 목록 / 내 정보 조회·수정) 단위테스트.
// 모드 전환·프로필 목록·세션 지원 흐름은 UserServiceSmokeTest / UserServiceSessionApplyTest가 커버한다.
@ExtendWith(MockitoExtension.class)
class UserServiceMyPageTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FanProfileRepository fanProfileRepository;
    @Mock
    private UserGenresRepository userGenresRepository;
    @Mock
    private UserRegionsRepository userRegionsRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private SessionPort sessionPort;
    @Mock
    private PerformancePort performancePort;
    @Mock
    private BandPort bandPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private NotifyPort notifyPort;

    private UserService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new UserService(
                userRepository,
                fanProfileRepository,
                userGenresRepository,
                userRegionsRepository,
                followPort,
                sessionPort,
                performancePort,
                bandPort,
                authPort,
                notifyPort
        );
    }

    private User user() {
        return StreamFixtures.fanUser(USER_ID);
    }

    private FanProfile fanProfile(User user, String nickname) {
        return FanProfile.builder().id(100L).user(user).nickname(nickname).build();
    }

    private UserGenres userGenre(User user, Genre genre) {
        return UserGenres.builder().user(user).genre(genre).build();
    }

    private UserRegions userRegion(User user, Region region) {
        return UserRegions.builder().user(user).region(region).build();
    }

    // ---------- getFanMyPage ----------

    @Test
    void getFanMyPage_팬프로필이_없으면_예외() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        UserException exception = assertThrows(UserException.class, () -> service.getFanMyPage(user));

        assertEquals(UserErrorCode.FAN_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getFanMyPage_팔로우한_밴드가_많은_관심장르가_대표장르가_된다() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "닉네임")));
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.INDIE), userGenre(user, Genre.METAL)));
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of(userRegion(user, Region.SEOUL)));
        when(followPort.countFollowedBandsGroupedByGenre(USER_ID))
                .thenReturn(Map.of(Genre.INDIE, 1L, Genre.METAL, 3L));

        FanMyPageResponse response = service.getFanMyPage(user);

        assertEquals(Genre.METAL, response.genre());
        assertEquals(1, response.additionalGenreCount());
    }

    @Test
    void getFanMyPage_팔로우_수가_동점이면_먼저_고른_관심장르가_대표장르가_된다() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "닉네임")));
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.JAZZ), userGenre(user, Genre.METAL)));
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of());
        when(followPort.countFollowedBandsGroupedByGenre(USER_ID))
                .thenReturn(Map.of(Genre.JAZZ, 2L, Genre.METAL, 2L));

        FanMyPageResponse response = service.getFanMyPage(user);

        assertEquals(Genre.JAZZ, response.genre());
    }

    @Test
    void getFanMyPage_관심장르가_아니면_팔로우한_밴드가_많아도_대표장르에서_제외된다() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "닉네임")));
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.INDIE)));
        when(userRegionsRepository.findAllByUser(user)).thenReturn(List.of());
        // METAL 밴드를 더 많이 팔로우했지만 관심장르로 등록하지 않음
        when(followPort.countFollowedBandsGroupedByGenre(USER_ID))
                .thenReturn(Map.of(Genre.METAL, 5L, Genre.INDIE, 1L));

        FanMyPageResponse response = service.getFanMyPage(user);

        assertEquals(Genre.INDIE, response.genre());
        assertEquals(0, response.additionalGenreCount());
    }

    @Test
    void getFanMyPage_닉네임_지역_카운트를_포함해_반환한다() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "세진")));
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.INDIE)));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(userRegion(user, Region.SEOUL), userRegion(user, Region.GYEONGGI)));
        when(followPort.countFollowedBandsGroupedByGenre(USER_ID)).thenReturn(Map.of());
        when(followPort.countFollowing(USER_ID)).thenReturn(7L);
        when(performancePort.countInterested(USER_ID)).thenReturn(4L);
        when(performancePort.countParticipated(USER_ID)).thenReturn(2L);

        FanMyPageResponse response = service.getFanMyPage(user);

        assertEquals("세진", response.nickname());
        assertEquals(List.of(Region.SEOUL, Region.GYEONGGI), response.regions());
        assertEquals(7L, response.followingCount());
        assertEquals(4L, response.interestedPerformanceCount());
        assertEquals(2L, response.participatedPerformanceCount());
    }

    // ---------- getParticipationHistory (연도 필터 → 날짜 범위 변환) ----------

    @Test
    void getParticipationHistory_THIS_YEAR는_올해_1월1일부터_12월31일까지_조회한다() {
        int baseYear = LocalDate.now().getYear();

        service.getParticipationHistory(USER_ID, HistoryYearFilter.THIS_YEAR, 0, 10);

        verify(performancePort).findParticipationHistory(
                eq(USER_ID), eq(HistoryYearFilter.THIS_YEAR), eq(baseYear),
                eq(LocalDate.of(baseYear, 1, 1)), eq(LocalDate.of(baseYear, 12, 31)), eq(0), eq(10));
    }

    @Test
    void getParticipationHistory_LAST_YEAR는_작년_범위로_조회한다() {
        int baseYear = LocalDate.now().getYear();

        service.getParticipationHistory(USER_ID, HistoryYearFilter.LAST_YEAR, 0, 10);

        verify(performancePort).findParticipationHistory(
                eq(USER_ID), eq(HistoryYearFilter.LAST_YEAR), eq(baseYear),
                eq(LocalDate.of(baseYear - 1, 1, 1)), eq(LocalDate.of(baseYear - 1, 12, 31)), eq(0), eq(10));
    }

    @Test
    void getParticipationHistory_BEFORE는_재작년_말_이전을_조회한다() {
        int baseYear = LocalDate.now().getYear();

        service.getParticipationHistory(USER_ID, HistoryYearFilter.BEFORE, 0, 10);

        verify(performancePort).findParticipationHistory(
                eq(USER_ID), eq(HistoryYearFilter.BEFORE), eq(baseYear),
                isNull(), eq(LocalDate.of(baseYear - 2, 12, 31)), eq(0), eq(10));
    }

    @Test
    void getParticipationHistory_필터가_null이면_ALL로_전체_조회한다() {
        int baseYear = LocalDate.now().getYear();

        service.getParticipationHistory(USER_ID, null, 0, 10);

        verify(performancePort).findParticipationHistory(
                eq(USER_ID), eq(HistoryYearFilter.ALL), eq(baseYear),
                isNull(), isNull(), eq(0), eq(10));
    }

    @Test
    void getParticipationHistory_페이지와_사이즈는_허용_범위로_보정된다() {
        int baseYear = LocalDate.now().getYear();

        // page 음수 → 0, size 100 → 상한 30
        service.getParticipationHistory(USER_ID, HistoryYearFilter.ALL, -1, 100);

        verify(performancePort).findParticipationHistory(
                eq(USER_ID), eq(HistoryYearFilter.ALL), eq(baseYear),
                isNull(), isNull(), eq(0), eq(30));
    }

    // ---------- getInterestedPerformances ----------

    @Test
    void getInterestedPerformances_연도_필터와_보정된_페이징으로_조회한다() {
        int baseYear = LocalDate.now().getYear();

        // size 0 → 최저 1로 보정
        service.getInterestedPerformances(USER_ID, HistoryYearFilter.THIS_YEAR, 2, 0);

        verify(performancePort).findInterestedPerformances(
                eq(USER_ID), eq(HistoryYearFilter.THIS_YEAR), eq(baseYear),
                eq(LocalDate.of(baseYear, 1, 1)), eq(LocalDate.of(baseYear, 12, 31)), eq(2), eq(1));
    }

    // ---------- getFollowedBands ----------

    @Test
    void getFollowedBands_보정된_페이징으로_팔로우_목록을_조회한다() {
        service.getFollowedBands(USER_ID, -5, 50);

        verify(followPort).findFollowedBands(USER_ID, 0, 30);
    }

    // ---------- getMyInfo ----------

    @Test
    void getMyInfo_팬프로필이_없으면_예외() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        UserException exception = assertThrows(UserException.class, () -> service.getMyInfo(user));

        assertEquals(UserErrorCode.FAN_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getMyInfo_장르와_지역을_선택_순서대로_코드값으로_반환한다() {
        User user = user();
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "세진")));
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.METAL), userGenre(user, Genre.INDIE)));
        when(userRegionsRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userRegion(user, Region.BUSAN)));

        MyInfoResponse response = service.getMyInfo(user);

        assertEquals("세진", response.nickname());
        assertEquals(List.of("METAL", "INDIE"), response.genres());
        assertEquals(List.of("BUSAN"), response.regions());
    }

    // ---------- updateMyInfo ----------

    private MyInfoUpdateRequest updateRequest() {
        return new MyInfoUpdateRequest("새닉네임", List.of(Genre.INDIE, Genre.METAL), List.of(Region.SEOUL));
    }

    @Test
    void updateMyInfo_사용자가_없으면_예외() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        UserException exception =
                assertThrows(UserException.class, () -> service.updateMyInfo(USER_ID, updateRequest()));

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void updateMyInfo_다른_사용자가_쓰는_닉네임이면_예외() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "기존닉네임")));
        when(fanProfileRepository.existsByNicknameAndUser_IdNot("새닉네임", USER_ID)).thenReturn(true);

        UserException exception =
                assertThrows(UserException.class, () -> service.updateMyInfo(USER_ID, updateRequest()));

        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, exception.getBaseResponseCode());
        verify(userGenresRepository, never()).deleteAllByUser(any());
        verify(userRegionsRepository, never()).deleteAllByUser(any());
    }

    @Test
    void updateMyInfo_검사와_커밋_사이_race로_unique_위반이_나면_중복_닉네임_예외로_변환한다() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "기존닉네임")));
        when(fanProfileRepository.existsByNicknameAndUser_IdNot("새닉네임", USER_ID)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("unique 위반")).when(fanProfileRepository).flush();

        UserException exception =
                assertThrows(UserException.class, () -> service.updateMyInfo(USER_ID, updateRequest()));

        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, exception.getBaseResponseCode());
        verify(userGenresRepository, never()).saveAll(anyList());
    }

    @Test
    void updateMyInfo_성공시_닉네임을_바꾸고_장르와_지역을_삭제_후_재저장한다() {
        User user = user();
        FanProfile fanProfile = fanProfile(user, "기존닉네임");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile));
        when(fanProfileRepository.existsByNicknameAndUser_IdNot("새닉네임", USER_ID)).thenReturn(false);
        when(userGenresRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userGenre(user, Genre.INDIE), userGenre(user, Genre.METAL)));
        when(userRegionsRepository.findAllByUserOrderByIdAsc(user))
                .thenReturn(List.of(userRegion(user, Region.SEOUL)));

        MyInfoResponse response = service.updateMyInfo(USER_ID, updateRequest());

        assertEquals("새닉네임", fanProfile.getNickname());
        assertEquals(List.of("INDIE", "METAL"), response.genres());
        assertEquals(List.of("SEOUL"), response.regions());

        // (user, genre) unique 제약 대비 : 삭제 → flush → 재저장 순서 보장 확인
        InOrder genreOrder = inOrder(userGenresRepository);
        genreOrder.verify(userGenresRepository).deleteAllByUser(user);
        genreOrder.verify(userGenresRepository).flush();
        genreOrder.verify(userGenresRepository).saveAll(anyList());

        InOrder regionOrder = inOrder(userRegionsRepository);
        regionOrder.verify(userRegionsRepository).deleteAllByUser(user);
        regionOrder.verify(userRegionsRepository).flush();
        regionOrder.verify(userRegionsRepository).saveAll(anyList());
    }

    @Test
    void updateMyInfo_요청에_중복된_장르가_있으면_한_번만_저장한다() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user, "기존닉네임")));
        when(fanProfileRepository.existsByNicknameAndUser_IdNot("새닉네임", USER_ID)).thenReturn(false);
        MyInfoUpdateRequest request = new MyInfoUpdateRequest(
                "새닉네임", List.of(Genre.INDIE, Genre.INDIE, Genre.METAL), List.of(Region.SEOUL, Region.SEOUL));

        service.updateMyInfo(USER_ID, request);

        ArgumentCaptor<List<UserGenres>> genresCaptor = ArgumentCaptor.captor();
        verify(userGenresRepository).saveAll(genresCaptor.capture());
        assertEquals(2, genresCaptor.getValue().size());

        ArgumentCaptor<List<UserRegions>> regionsCaptor = ArgumentCaptor.captor();
        verify(userRegionsRepository).saveAll(regionsCaptor.capture());
        assertEquals(1, regionsCaptor.getValue().size());
    }
}
