package com.umc.bscene.domain.user.service;

import com.umc.bscene.domain.user.dto.request.UserModeUpdateRequest;
import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.dto.response.mypage.BandMyPageResponse;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.global.response.CursorPage;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSmokeTest {

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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
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

    @Test
    @DisplayName("온보딩을 마치지 않은 사용자는 내 프로필을 조회할 수 없다")
    void findMyProfilesFailsWhenOnboardingNotCompleted() {
        User user = StreamFixtures.user(1L, null);

        UserException exception = assertThrows(
                UserException.class,
                () -> userService.findMyProfiles(user, "all")
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.ONBOARDING_NOT_COMPLETED);
    }

    @Test
    @DisplayName("팬 모드에서 밴드 모드로 전환하면 현재 모드가 BAND로 바뀌고 밴드 프로필이 활성화된다")
    void updateModeSwitchesFanToBand() {
        User user = StreamFixtures.fanUser(1L);
        User found = StreamFixtures.fanUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(found));

        userService.updateMode(user, new UserModeUpdateRequest(10L, UserMode.BAND));

        assertThat(found.getCurrentMode()).isEqualTo(UserMode.BAND);
        verify(bandPort).changeProfileByProfileId(1L, 10L);
    }

    @Test
    @DisplayName("밴드 모드에서 팬 모드로 전환하면 활성 밴드 프로필을 비활성화하고 현재 모드가 FAN으로 바뀐다")
    void updateModeSwitchesBandToFan() {
        User user = StreamFixtures.bandUser(1L);
        User found = StreamFixtures.bandUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(found));

        userService.updateMode(user, new UserModeUpdateRequest(10L, UserMode.FAN));

        assertThat(found.getCurrentMode()).isEqualTo(UserMode.FAN);
        verify(bandPort).deactivateCurrentActiveProfile(1L);
        verify(bandPort, never()).changeProfileByProfileId(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 모드 변경 요청은 실패한다")
    void updateModeFailsWhenUserNotFound() {
        User user = StreamFixtures.fanUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserException exception = assertThrows(
                UserException.class,
                () -> userService.updateMode(user, new UserModeUpdateRequest(10L, UserMode.BAND))
        );

        assertThat(exception.getBaseResponseCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
        verify(bandPort, never()).changeProfileByProfileId(any(), any());
    }

    @Test
    @DisplayName("이미 밴드 모드인 사용자가 밴드 프로필을 바꾸면 모드 변경 없이 활성 프로필만 전환된다")
    void updateModeBandToBandOnlySwitchesProfile() {
        User user = StreamFixtures.bandUser(1L);
        User found = StreamFixtures.bandUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(found));

        userService.updateMode(user, new UserModeUpdateRequest(10L, UserMode.BAND));

        assertThat(found.getCurrentMode()).isEqualTo(UserMode.BAND);
        verify(bandPort).changeProfileByProfileId(1L, 10L);
        verify(bandPort, never()).deactivateCurrentActiveProfile(any());
    }

    @Test
    @DisplayName("이미 팬 모드인 사용자의 팬 모드 전환 요청은 아무것도 바꾸지 않는다")
    void updateModeFanToFanIsNoOp() {
        User user = StreamFixtures.fanUser(1L);
        User found = StreamFixtures.fanUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(found));

        userService.updateMode(user, new UserModeUpdateRequest(10L, UserMode.FAN));

        assertThat(found.getCurrentMode()).isEqualTo(UserMode.FAN);
        verify(bandPort, never()).changeProfileByProfileId(any(), any());
        verify(bandPort, never()).deactivateCurrentActiveProfile(any());
    }

    @Test
    @DisplayName("밴드 마이페이지는 활성 밴드 멤버 프로필 정보를 현재 모드와 함께 매핑해 내려준다")
    void getBandMyPageMapsActiveBandMemberProfile() {
        User user = StreamFixtures.bandUser(1L);
        when(bandPort.getActiveBandMemberProfile(1L)).thenReturn(new BandMemberResponse(
                10L, "https://cdn.test/profile.jpg", "예명", "밴드이름",
                List.of("기타", "보컬"), 3, 2, 5, true
        ));

        BandMyPageResponse response = userService.getBandMyPage(user);

        assertThat(response.bandMemberProfileId()).isEqualTo(10L);
        assertThat(response.nickname()).isEqualTo("예명");
        assertThat(response.bandName()).isEqualTo("밴드이름");
        assertThat(response.parts()).containsExactly("기타", "보컬");
        assertThat(response.currentMode()).isEqualTo(UserMode.BAND);
        assertThat(response.follower()).isEqualTo(3L);
        assertThat(response.applicant()).isEqualTo(2L);
        assertThat(response.performance()).isEqualTo(5L);
        assertThat(response.isBandMember()).isTrue();
    }

    @Test
    @DisplayName("all 타입 프로필 조회는 로컬 자격증명이 있으면 로컬 이메일을 담아 팬 프로필을 내려준다")
    void findMyProfilesAllUsesLocalEmailFirst() {
        User user = StreamFixtures.fanUser(1L);
        when(bandPort.getAssociatedBandProfiles(1L)).thenReturn(List.of());
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user)));
        when(authPort.hasLocalCredential(1L)).thenReturn(true);
        when(authPort.getEmailToLocalCredential(1L)).thenReturn("local@test.com");

        MyProfileResponse response = userService.findMyProfiles(user, "all");

        assertThat(response.fanProfile().email()).isEqualTo("local@test.com");
        assertThat(response.fanProfile().nickname()).isEqualTo(NICKNAME);
        assertThat(response.fanProfile().isActive()).isTrue();
        verify(authPort, never()).getEmailToOauthAccount(any());
    }

    @Test
    @DisplayName("all 타입 프로필 조회는 로컬 자격증명이 없으면 소셜 계정 이메일로 대신한다")
    void findMyProfilesAllFallsBackToOauthEmail() {
        User user = StreamFixtures.bandUser(1L);
        when(bandPort.getAssociatedBandProfiles(1L)).thenReturn(List.of());
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.of(fanProfile(user)));
        when(authPort.hasLocalCredential(1L)).thenReturn(false);
        when(authPort.hasOauthAccount(1L)).thenReturn(true);
        when(authPort.getEmailToOauthAccount(1L)).thenReturn("oauth@test.com");

        MyProfileResponse response = userService.findMyProfiles(user, "all");

        assertThat(response.fanProfile().email()).isEqualTo("oauth@test.com");
        assertThat(response.fanProfile().isActive()).isFalse();
    }

    @Test
    @DisplayName("all 타입 프로필 조회에서 팬 프로필이 없으면 팬 프로필은 null로 내려간다")
    void findMyProfilesAllWithoutFanProfileReturnsNullFanProfile() {
        User user = StreamFixtures.fanUser(1L);
        when(bandPort.getAssociatedBandProfiles(1L)).thenReturn(List.of());
        when(fanProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        MyProfileResponse response = userService.findMyProfiles(user, "all");

        assertThat(response.fanProfile()).isNull();
    }

    @Test
    @DisplayName("all이 아닌 타입의 프로필 조회는 팬 프로필 없이 밴드 프로필만 내려준다")
    void findMyProfilesNonAllReturnsBandProfilesOnly() {
        User user = StreamFixtures.fanUser(1L);
        when(bandPort.getAssociatedBandProfiles(1L)).thenReturn(List.of());

        MyProfileResponse response = userService.findMyProfiles(user, "band");

        assertThat(response.fanProfile()).isNull();
        verify(fanProfileRepository, never()).findByUser(any(User.class));
        verify(authPort, never()).hasLocalCredential(any());
    }

    @Test
    @DisplayName("받은 모집 공고 조회는 활성 밴드 ID로 위임하고 페이지 크기를 1~15로 보정한다")
    void findMyBandsRecruitmentsClampsPageSize() {
        User user = StreamFixtures.bandUser(1L);
        CursorPage<SessionRecruitmentResponse> page = CursorPage.ofLastPage(List.of());
        when(bandPort.getActiveBandMemberProfile_BandIdIdByUserId(1L)).thenReturn(7L);
        when(sessionPort.findRecruitmentsByBandId(7L, RecruitmentStatusFilter.OPEN, 99L, 15))
                .thenReturn(page);

        assertThat(userService.findMyBandsRecruitments(user, RecruitmentStatusFilter.OPEN, 99L, 100))
                .isSameAs(page);
    }

    @Test
    @DisplayName("받은 모집 공고 조회의 페이지 크기는 최소 1로 보정된다")
    void findMyBandsRecruitmentsRaisesSizeToMinimum() {
        User user = StreamFixtures.bandUser(1L);
        CursorPage<SessionRecruitmentResponse> page = CursorPage.ofLastPage(List.of());
        when(bandPort.getActiveBandMemberProfile_BandIdIdByUserId(1L)).thenReturn(7L);
        when(sessionPort.findRecruitmentsByBandId(7L, RecruitmentStatusFilter.CLOSE, null, 1))
                .thenReturn(page);

        assertThat(userService.findMyBandsRecruitments(user, RecruitmentStatusFilter.CLOSE, null, 0))
                .isSameAs(page);
    }

    private static final String NICKNAME = "밴신";

    private static FanProfile fanProfile(User user) {
        return FanProfile.builder()
                .id(1L)
                .user(user)
                .nickname(NICKNAME)
                .build();
    }
}
