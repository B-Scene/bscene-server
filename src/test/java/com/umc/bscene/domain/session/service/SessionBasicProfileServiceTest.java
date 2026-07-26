package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.auth.entity.credential.LocalCredential;
import com.umc.bscene.domain.auth.repository.credential.LocalCredentialRepository;
import com.umc.bscene.domain.oauth.entity.OauthAccount;
import com.umc.bscene.domain.oauth.repository.OauthAccountRepository;
import com.umc.bscene.domain.session.dto.profile.request.SessionBasicProfileUpdateRequest;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionBasicProfileServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionBasicProfileRepository profileRepository;
    @Mock
    private LocalCredentialRepository localCredentialRepository;
    @Mock
    private OauthAccountRepository oauthAccountRepository;
    @Mock
    private SessionBasicProfileUpdateRequest request;

    private SessionBasicProfileService service;

    @BeforeEach
    void setUp() {
        service = new SessionBasicProfileService(
                userRepository,
                profileRepository,
                localCredentialRepository,
                oauthAccountRepository
        );
    }

    @Test
    @DisplayName("저장된 세션 기본정보를 조회한다")
    void getProfileUsesSavedProfile() {
        User user = user();
        SessionBasicProfile profile = SessionBasicProfile.builder()
                .user(user)
                .email("session@example.com")
                .gender(Gender.FEMALE)
                .profileImageUrl("profile.jpg")
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));
        when(localCredentialRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(LocalCredential.builder()
                        .user(user)
                        .loginId("account@example.com")
                        .build()));

        var response = service.getProfile(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo("session@example.com");
        assertThat(response.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(response.getProfileImageUrl()).isEqualTo("profile.jpg");
        verify(oauthAccountRepository, never())
                .findFirstByUser_IdOrderByIdAsc(USER_ID);
    }

    @Test
    @DisplayName("세션 이메일이 없으면 로컬 계정 이메일을 사용한다")
    void getProfileFallsBackToLocalAccountEmail() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(localCredentialRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(LocalCredential.builder()
                        .user(user)
                        .loginId("local@example.com")
                        .build()));

        var response = service.getProfile(USER_ID);

        assertThat(response.getEmail()).isEqualTo("local@example.com");
        assertThat(response.getGender()).isEqualTo(user.getGender());
        assertThat(response.getBirthDate()).isEqualTo(user.getBirthDate());
    }

    @Test
    @DisplayName("로컬 로그인 아이디가 이메일이 아니면 OAuth 계정 이메일을 사용한다")
    void getProfileFallsBackToOauthEmail() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(localCredentialRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(LocalCredential.builder()
                        .user(user)
                        .loginId("local-id")
                        .build()));
        when(oauthAccountRepository.findFirstByUser_IdOrderByIdAsc(USER_ID))
                .thenReturn(Optional.of(OauthAccount.builder()
                        .user(user)
                        .email("oauth@example.com")
                        .build()));

        var response = service.getProfile(USER_ID);

        assertThat(response.getEmail()).isEqualTo("oauth@example.com");
    }

    @Test
    @DisplayName("기존 세션 기본정보를 수정한다")
    void updateExistingProfile() {
        User user = user();
        SessionBasicProfile profile = SessionBasicProfile.builder()
                .user(user)
                .email("old@example.com")
                .gender(Gender.MALE)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(profile));
        when(request.getEmail()).thenReturn("  new@example.com  ");
        when(request.getGender()).thenReturn(Gender.FEMALE);
        when(request.getProfileImageUrl()).thenReturn("  profile.jpg  ");
        when(profileRepository.save(profile)).thenReturn(profile);
        when(localCredentialRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(oauthAccountRepository.findFirstByUser_IdOrderByIdAsc(USER_ID))
                .thenReturn(Optional.empty());

        var response = service.updateProfile(USER_ID, request);

        verify(profileRepository).save(profile);
        assertThat(profile.getEmail()).isEqualTo("new@example.com");
        assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.getProfileImageUrl()).isEqualTo("profile.jpg");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("기존 정보가 없으면 세션 기본정보를 새로 생성한다")
    void updateCreatesProfileWhenMissing() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(request.getEmail()).thenReturn("new@example.com");
        when(request.getGender()).thenReturn(Gender.FEMALE);
        when(request.getProfileImageUrl()).thenReturn("");
        when(profileRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(localCredentialRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());
        when(oauthAccountRepository.findFirstByUser_IdOrderByIdAsc(USER_ID))
                .thenReturn(Optional.empty());

        service.updateProfile(USER_ID, request);

        ArgumentCaptor<SessionBasicProfile> captor =
                ArgumentCaptor.forClass(SessionBasicProfile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("인증 사용자가 없으면 기본정보 조회에 실패한다")
    void getProfileFailsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(USER_ID))
                .isInstanceOf(BaseException.class);
    }

    private User user() {
        return User.builder()
                .id(USER_ID)
                .name("사용자")
                .phone("01012345678")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
    }
}
