package com.umc.bscene.domain.auth.service.onboarding;

import com.umc.bscene.domain.auth.dto.onboarding.request.OnboardingSaveRequest;
import com.umc.bscene.domain.auth.dto.onboarding.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.GenreResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.dto.onboarding.response.RegionResponse;
import com.umc.bscene.domain.auth.enums.code.OnboardingErrorCode;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.auth.exception.onboarding.OnboardingException;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserAvailableModes;
import com.umc.bscene.domain.user.entity.UserGenres;
import com.umc.bscene.domain.user.entity.UserRegions;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserAvailableModesRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FanProfileRepository fanProfileRepository;

    @Mock
    private UserAvailableModesRepository userAvailableModesRepository;

    @Mock
    private UserGenresRepository userGenresRepository;

    @Mock
    private UserRegionsRepository userRegionsRepository;

    private OnboardingService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(
                userRepository,
                fanProfileRepository,
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    // ---------- genre and region ----------

    @Test
    void getGenres_전체_장르를_코드와_한글이름으로_반환한다() {
        List<GenreResponse> response = service.getGenres();

        List<GenreResponse> expected = Arrays.stream(Genre.values())
                .map(genre -> new GenreResponse(
                        genre.name(),
                        genre.getName()
                ))
                .toList();

        assertThat(response).containsExactlyElementsOf(expected);
    }

    @Test
    void getRegions_전체_지역을_코드와_한글이름으로_반환한다() {
        List<RegionResponse> response = service.getRegions();

        List<RegionResponse> expected = Arrays.stream(Region.values())
                .map(region -> new RegionResponse(
                        region.name(),
                        region.getName()
                ))
                .toList();

        assertThat(response).containsExactlyElementsOf(expected);
    }

    // ---------- getMyOnboardingStatus ----------

    @Test
    void getMyOnboardingStatus_선택정보가_없으면_필요한_단계를_반환한다() {
        User user = incompleteUser();

        when(fanProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());
        when(userAvailableModesRepository.findAllByUser(user))
                .thenReturn(List.of());
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of());
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of());

        OnboardingStatusResponse response =
                service.getMyOnboardingStatus(new AuthMember(user));

        assertThat(response.completed()).isFalse();
        assertThat(response.currentMode()).isNull();
        assertThat(response.availableModes()).isEmpty();
        assertThat(response.fanNickname()).isNull();
        assertThat(response.selectedGenres()).isEmpty();
        assertThat(response.selectedRegions()).isEmpty();
        assertThat(response.requiredSteps()).containsExactly(
                "MODE",
                "AVAILABLE_MODE",
                "GENRE",
                "REGION"
        );
    }

    @Test
    void getMyOnboardingStatus_팬모드인데_프로필이_없으면_닉네임_단계가_필요하다() {
        User user = incompleteUser();
        UserAvailableModes fanMode = availableMode(
                user,
                UserMode.FAN
        );

        when(fanProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());
        when(userAvailableModesRepository.findAllByUser(user))
                .thenReturn(List.of(fanMode));
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(userGenre(user, Genre.INDIE)));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(userRegion(user, Region.SEOUL)));

        OnboardingStatusResponse response =
                service.getMyOnboardingStatus(new AuthMember(user));

        assertThat(response.requiredSteps()).containsExactly(
                "MODE",
                "FAN_NICKNAME"
        );
    }

    @Test
    void getMyOnboardingStatus_온보딩이_완료됐으면_저장된_상태를_반환한다() {
        User user = completedUser();
        FanProfile fanProfile = fanProfile(user, "테스트팬");

        when(fanProfileRepository.findByUser(user))
                .thenReturn(Optional.of(fanProfile));
        when(userAvailableModesRepository.findAllByUser(user))
                .thenReturn(List.of(
                        availableMode(user, UserMode.FAN),
                        availableMode(user, UserMode.BAND)
                ));
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(
                        userGenre(user, Genre.INDIE),
                        userGenre(user, Genre.JAZZ)
                ));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(userRegion(user, Region.SEOUL)));

        OnboardingStatusResponse response =
                service.getMyOnboardingStatus(new AuthMember(user));

        assertThat(response.completed()).isTrue();
        assertThat(response.currentMode()).isEqualTo(UserMode.FAN);
        assertThat(response.availableModes()).containsExactly(
                UserMode.FAN,
                UserMode.BAND
        );
        assertThat(response.fanNickname()).isEqualTo("테스트팬");
        assertThat(response.selectedGenres()).containsExactly(
                new GenreResponse("INDIE", "인디"),
                new GenreResponse("JAZZ", "재즈")
        );
        assertThat(response.selectedRegions()).containsExactly(
                new RegionResponse("SEOUL", "서울")
        );
        assertThat(response.requiredSteps()).isEmpty();
    }

    // ---------- saveOnboarding ----------

    @Test
    void saveOnboarding_사용자가_존재하지_않으면_예외() {
        User detachedUser = incompleteUser();
        OnboardingSaveRequest request = fanOnboardingRequest();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        OnboardingException exception = assertThrows(
                OnboardingException.class,
                () -> service.saveOnboarding(
                        new AuthMember(detachedUser),
                        request
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(OnboardingErrorCode.USER_NOT_FOUND);
        verifyNoInteractions(
                fanProfileRepository,
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    @Test
    void saveOnboarding_이미_완료한_사용자이면_예외() {
        User user = completedUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        OnboardingException exception = assertThrows(
                OnboardingException.class,
                () -> service.saveOnboarding(
                        new AuthMember(user),
                        fanOnboardingRequest()
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(OnboardingErrorCode.ALREADY_ONBOARDED);
        verifyNoInteractions(
                fanProfileRepository,
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    @Test
    void saveOnboarding_초기모드가_선택한_모드에_없으면_예외() {
        User user = incompleteUser();
        OnboardingSaveRequest request = new OnboardingSaveRequest(
                List.of(UserMode.FAN),
                UserMode.BAND,
                "테스트팬",
                List.of(Genre.INDIE),
                List.of(Region.SEOUL)
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        OnboardingException exception = assertThrows(
                OnboardingException.class,
                () -> service.saveOnboarding(
                        new AuthMember(user),
                        request
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(OnboardingErrorCode.INVALID_CURRENT_MODE);
        verifyNoInteractions(
                fanProfileRepository,
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void saveOnboarding_팬모드인데_닉네임이_없으면_예외(
            String fanNickname
    ) {
        User user = incompleteUser();
        OnboardingSaveRequest request = new OnboardingSaveRequest(
                List.of(UserMode.FAN),
                UserMode.FAN,
                fanNickname,
                List.of(Genre.INDIE),
                List.of(Region.SEOUL)
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        OnboardingException exception = assertThrows(
                OnboardingException.class,
                () -> service.saveOnboarding(
                        new AuthMember(user),
                        request
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(OnboardingErrorCode.FAN_NICKNAME_REQUIRED);
        verify(fanProfileRepository, never())
                .existsByNickname(any());
        verifyNoInteractions(
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    @Test
    void saveOnboarding_팬_닉네임이_중복이면_예외() {
        User user = incompleteUser();
        OnboardingSaveRequest request = fanOnboardingRequest();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(fanProfileRepository.existsByNickname(
                request.fanNickname()
        )).thenReturn(true);

        OnboardingException exception = assertThrows(
                OnboardingException.class,
                () -> service.saveOnboarding(
                        new AuthMember(user),
                        request
                )
        );

        assertThat(exception.getBaseResponseCode())
                .isEqualTo(OnboardingErrorCode.DUPLICATE_FAN_NICKNAME);
        verify(fanProfileRepository, never())
                .save(any(FanProfile.class));
        verifyNoInteractions(
                userAvailableModesRepository,
                userGenresRepository,
                userRegionsRepository
        );
    }

    @Test
    void saveOnboarding_팬과_밴드모드를_저장하고_중복선택을_제거한다() {
        User user = incompleteUser();
        OnboardingSaveRequest request = new OnboardingSaveRequest(
                List.of(UserMode.FAN, UserMode.BAND, UserMode.FAN),
                UserMode.BAND,
                "테스트팬",
                List.of(Genre.INDIE, Genre.JAZZ, Genre.INDIE),
                List.of(Region.SEOUL, Region.SEOUL)
        );
        FanProfile fanProfile = fanProfile(user, "테스트팬");

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(fanProfileRepository.existsByNickname("테스트팬"))
                .thenReturn(false);
        when(fanProfileRepository.findByUser(user))
                .thenReturn(Optional.of(fanProfile));
        when(userAvailableModesRepository.findAllByUser(user))
                .thenReturn(List.of(
                        availableMode(user, UserMode.FAN),
                        availableMode(user, UserMode.BAND)
                ));
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(
                        userGenre(user, Genre.INDIE),
                        userGenre(user, Genre.JAZZ)
                ));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(userRegion(user, Region.SEOUL)));

        OnboardingStatusResponse response = service.saveOnboarding(
                new AuthMember(user),
                request
        );

        assertThat(user.getOnboardingCompleted()).isTrue();
        assertThat(user.getCurrentMode()).isEqualTo(UserMode.BAND);
        assertThat(response.completed()).isTrue();
        assertThat(response.currentMode()).isEqualTo(UserMode.BAND);
        assertThat(response.fanNickname()).isEqualTo("테스트팬");
        assertThat(response.requiredSteps()).isEmpty();

        ArgumentCaptor<FanProfile> fanProfileCaptor =
                ArgumentCaptor.forClass(FanProfile.class);
        verify(fanProfileRepository).save(fanProfileCaptor.capture());
        assertThat(fanProfileCaptor.getValue().getUser()).isSameAs(user);
        assertThat(fanProfileCaptor.getValue().getNickname())
                .isEqualTo("테스트팬");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserAvailableModes>> modesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(userAvailableModesRepository)
                .saveAll(modesCaptor.capture());
        assertThat(modesCaptor.getValue())
                .extracting(UserAvailableModes::getMode)
                .containsExactly(UserMode.FAN, UserMode.BAND);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserGenres>> genresCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(userGenresRepository).saveAll(genresCaptor.capture());
        assertThat(genresCaptor.getValue())
                .extracting(UserGenres::getGenre)
                .containsExactly(Genre.INDIE, Genre.JAZZ);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserRegions>> regionsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(userRegionsRepository).saveAll(regionsCaptor.capture());
        assertThat(regionsCaptor.getValue())
                .extracting(UserRegions::getRegion)
                .containsExactly(Region.SEOUL);
    }

    @Test
    void saveOnboarding_밴드모드만_선택하면_팬프로필을_만들지_않는다() {
        User user = incompleteUser();
        OnboardingSaveRequest request = new OnboardingSaveRequest(
                List.of(UserMode.BAND),
                UserMode.BAND,
                null,
                List.of(Genre.HARD_ROCK),
                List.of(Region.BUSAN)
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(fanProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());
        when(userAvailableModesRepository.findAllByUser(user))
                .thenReturn(List.of(availableMode(user, UserMode.BAND)));
        when(userGenresRepository.findAllByUser(user))
                .thenReturn(List.of(
                        userGenre(user, Genre.HARD_ROCK)
                ));
        when(userRegionsRepository.findAllByUser(user))
                .thenReturn(List.of(
                        userRegion(user, Region.BUSAN)
                ));

        OnboardingStatusResponse response = service.saveOnboarding(
                new AuthMember(user),
                request
        );

        assertThat(response.completed()).isTrue();
        assertThat(response.currentMode()).isEqualTo(UserMode.BAND);
        assertThat(response.fanNickname()).isNull();
        assertThat(response.requiredSteps()).isEmpty();
        verify(fanProfileRepository, never())
                .existsByNickname(any());
        verify(fanProfileRepository, never())
                .save(any(FanProfile.class));
    }

    // ---------- checkFanNickname ----------

    @Test
    void checkFanNickname_사용하지_않는_닉네임이면_사용_가능하다() {
        when(fanProfileRepository.existsByNickname("사용가능닉네임"))
                .thenReturn(false);

        FanNicknameCheckResponse response =
                service.checkFanNickname("사용가능닉네임");

        assertThat(response.available()).isTrue();
    }

    @Test
    void checkFanNickname_이미_사용중인_닉네임이면_사용_불가능하다() {
        when(fanProfileRepository.existsByNickname("중복닉네임"))
                .thenReturn(true);

        FanNicknameCheckResponse response =
                service.checkFanNickname("중복닉네임");

        assertThat(response.available()).isFalse();
    }

    private User incompleteUser() {
        return User.builder()
                .id(USER_ID)
                .name("테스트유저")
                .birthDate(LocalDate.of(1999, 1, 1))
                .gender(Gender.MALE)
                .phone("01012345678")
                .onboardingCompleted(false)
                .build();
    }

    private User completedUser() {
        return User.builder()
                .id(USER_ID)
                .name("테스트유저")
                .birthDate(LocalDate.of(1999, 1, 1))
                .gender(Gender.MALE)
                .phone("01012345678")
                .currentMode(UserMode.FAN)
                .onboardingCompleted(true)
                .build();
    }

    private FanProfile fanProfile(User user, String nickname) {
        return FanProfile.builder()
                .id(200L)
                .user(user)
                .nickname(nickname)
                .build();
    }

    private UserAvailableModes availableMode(
            User user,
            UserMode mode
    ) {
        return UserAvailableModes.builder()
                .user(user)
                .mode(mode)
                .build();
    }

    private UserGenres userGenre(User user, Genre genre) {
        return UserGenres.builder()
                .user(user)
                .genre(genre)
                .build();
    }

    private UserRegions userRegion(User user, Region region) {
        return UserRegions.builder()
                .user(user)
                .region(region)
                .build();
    }

    private OnboardingSaveRequest fanOnboardingRequest() {
        return new OnboardingSaveRequest(
                List.of(UserMode.FAN),
                UserMode.FAN,
                "테스트팬",
                List.of(Genre.INDIE),
                List.of(Region.SEOUL)
        );
    }
}
