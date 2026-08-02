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
import com.umc.bscene.domain.user.entity.*;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.*;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final FanProfileRepository fanProfileRepository;
    private final UserAvailableModesRepository userAvailableModesRepository;
    private final UserGenresRepository userGenresRepository;
    private final UserRegionsRepository userRegionsRepository;

    // 장르 목록 조회
    public List<GenreResponse> getGenres() {
        return Arrays.stream(Genre.values())
                .map(genre -> new GenreResponse(
                        genre.name(),
                        genre.getName()
                ))
                .toList();
    }

    // 지역 목록 조회
    public List<RegionResponse> getRegions() {
        return Arrays.stream(Region.values())
                .map(region -> new RegionResponse(
                        region.name(),
                        region.getName()
                ))
                .toList();
    }

    // 내 온보딩 상태 조회
    public OnboardingStatusResponse getMyOnboardingStatus(AuthMember authMember) {
        return buildStatus(authMember.getUser());
    }

    // 온보딩 정보 저장.
    // FAN 포함 시 : 닉네임/장르/지역까지 저장하고 온보딩 완료 처리.
    // BAND만 선택 시 : 팬 정보는 받지 않고 모드만 저장한 뒤 미완료 상태로 앱에 진입한다.
    //                  (온보딩 완료 = 팬 온보딩 완료. 이후 팬모드 전환 시 온보딩에 재진입해 나머지를 저장)
    @Transactional
    public OnboardingStatusResponse saveOnboarding(AuthMember authMember, OnboardingSaveRequest request) {
        // 인증 필터에서 로드된 User는 detached 상태 → 변경 감지를 위해 트랜잭션 내에서 재조회
        User user = userRepository.findById(authMember.getUser().getId())
                .orElseThrow(() -> new OnboardingException(OnboardingErrorCode.USER_NOT_FOUND));

        // 이미 온보딩을 완료한 유저의 재호출은 중복 저장(unique 제약)으로 이어지므로 차단.
        // BAND만 선택한 유저는 미완료라 재진입 가능
        if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            throw new OnboardingException(OnboardingErrorCode.ALREADY_ONBOARDED);
        }

        if (!request.selectedModes().contains(request.initialMode())) {
            throw new OnboardingException(OnboardingErrorCode.INVALID_CURRENT_MODE);
        }

        if (!request.selectedModes().contains(UserMode.FAN)) {
            saveAvailableModes(user, request.selectedModes());
            user.changeMode(request.initialMode());
            return buildStatus(user);
        }

        validateFanSelections(request);
        applyFanProfile(user, request.fanNickname());
        saveAvailableModes(user, request.selectedModes());
        saveGenres(user, request.genres());
        saveRegions(user, request.regions());

        user.completeOnboarding(request.initialMode());

        return buildStatus(user);
    }

    // 팬 닉네임 중복 확인
    public FanNicknameCheckResponse checkFanNickname(String nickname) {
        boolean exists = fanProfileRepository.existsByNickname(nickname);

        return new FanNicknameCheckResponse(!exists);
    }

    private OnboardingStatusResponse buildStatus(User user) {
        Optional<FanProfile> fanProfile = fanProfileRepository.findByUser(user);

        String fanNickname = fanProfile
                .map(FanProfile::getNickname)
                .orElse(null);

        List<UserMode> availableModes = userAvailableModesRepository.findAllByUser(user).stream()
                .map(UserAvailableModes::getMode)
                .toList();

        List<GenreResponse> selectedGenres = userGenresRepository.findAllByUser(user).stream()
                .map(userGenre -> new GenreResponse(
                        userGenre.getGenre().name(),
                        userGenre.getGenre().getName()
                ))
                .toList();

        List<RegionResponse> selectedRegions = userRegionsRepository.findAllByUser(user).stream()
                .map(userRegion -> new RegionResponse(
                        userRegion.getRegion().name(),
                        userRegion.getRegion().getName()
                ))
                .toList();

        List<String> requiredSteps = getRequiredSteps(
                user,
                availableModes,
                fanProfile.isPresent(),
                selectedGenres,
                selectedRegions
        );

        return new OnboardingStatusResponse(
                user.getOnboardingCompleted(),
                user.getCurrentMode(),
                availableModes,
                fanNickname,
                selectedGenres,
                selectedRegions,
                requiredSteps
        );
    }

    private List<String> getRequiredSteps(
            User user,
            List<UserMode> availableModes,
            boolean hasFanProfile,
            List<GenreResponse> selectedGenres,
            List<RegionResponse> selectedRegions
    ) {
        List<String> requiredSteps = new ArrayList<>();

        if (user.getCurrentMode() == null) {
            requiredSteps.add("MODE");
        }

        if (availableModes.isEmpty()) {
            requiredSteps.add("AVAILABLE_MODE");
        }

        // 닉네임/장르/지역은 팬 온보딩 정보 → FAN 모드가 없는 유저에게는 필요한 단계가 아니다.
        // (BAND만 선택한 미완료 유저는 남은 단계 없이 앱을 사용하다가 팬모드 전환 시 재진입)
        if (availableModes.contains(UserMode.FAN)) {
            if (!hasFanProfile) {
                requiredSteps.add("FAN_NICKNAME");
            }

            if (selectedGenres.isEmpty()) {
                requiredSteps.add("GENRE");
            }

            if (selectedRegions.isEmpty()) {
                requiredSteps.add("REGION");
            }
        }

        return requiredSteps;
    }

    // FAN 포함 시 장르/지역 필수. (BAND만 선택하는 케이스 때문에 DTO @NotEmpty로는 검증 불가)
    private void validateFanSelections(OnboardingSaveRequest request) {
        if (request.genres() == null || request.genres().isEmpty()) {
            throw new OnboardingException(OnboardingErrorCode.GENRE_REQUIRED);
        }

        if (request.regions() == null || request.regions().isEmpty()) {
            throw new OnboardingException(OnboardingErrorCode.REGION_REQUIRED);
        }
    }

    // 팬 프로필(닉네임) 생성. 닉네임 필수 + 중복 검증
    private void applyFanProfile(User user, String fanNickname) {
        if (fanNickname == null || fanNickname.isBlank()) {
            throw new OnboardingException(OnboardingErrorCode.FAN_NICKNAME_REQUIRED);
        }

        if (fanProfileRepository.existsByNickname(fanNickname)) {
            throw new OnboardingException(OnboardingErrorCode.DUPLICATE_FAN_NICKNAME);
        }

        fanProfileRepository.save(FanProfile.builder()
                .user(user)
                .nickname(fanNickname)
                .build());
    }

    // BAND로만 시작한 유저가 팬모드를 추가하러 재진입하는 경우, 첫 온보딩 때 저장된 BAND 행이
    // 이미 있으므로(unique 제약) 저장된 모드는 건너뛰고 새 모드만 추가한다.
    private void saveAvailableModes(User user, List<UserMode> modes) {
        List<UserMode> existingModes = userAvailableModesRepository.findAllByUser(user).stream()
                .map(UserAvailableModes::getMode)
                .toList();

        List<UserAvailableModes> entities = modes.stream()
                .distinct()
                .filter(mode -> !existingModes.contains(mode))
                .map(mode -> UserAvailableModes.builder().user(user).mode(mode).build())
                .toList();
        userAvailableModesRepository.saveAll(entities);
    }

    private void saveGenres(User user, List<Genre> genres) {
        List<UserGenres> entities = genres.stream()
                .distinct()
                .map(genre -> UserGenres.builder().user(user).genre(genre).build())
                .toList();
        userGenresRepository.saveAll(entities);
    }

    private void saveRegions(User user, List<Region> regions) {
        List<UserRegions> entities = regions.stream()
                .distinct()
                .map(region -> UserRegions.builder().user(user).region(region).build())
                .toList();
        userRegionsRepository.saveAll(entities);
    }
}
