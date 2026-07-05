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
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserAvailableModesRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
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

    // 온보딩 정보 저장 (모드/닉네임/장르/지역 저장 후 온보딩 완료 처리)
    @Transactional
    public OnboardingStatusResponse saveOnboarding(AuthMember authMember, OnboardingSaveRequest request) {
        // 인증 필터에서 로드된 User는 detached 상태 → 변경 감지를 위해 트랜잭션 내에서 재조회
        User user = userRepository.findById(authMember.getUser().getId())
                .orElseThrow(() -> new OnboardingException(OnboardingErrorCode.USER_NOT_FOUND));

        // 이미 온보딩을 완료한 유저의 재호출은 중복 저장(unique 제약)으로 이어지므로 차단
        if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            throw new OnboardingException(OnboardingErrorCode.ALREADY_ONBOARDED);
        }

        if (!request.selectedModes().contains(request.initialMode())) {
            throw new OnboardingException(OnboardingErrorCode.INVALID_CURRENT_MODE);
        }

        applyFanProfile(user, request.selectedModes(), request.fanNickname());
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

        if (availableModes.contains(UserMode.FAN) && !hasFanProfile) {
            requiredSteps.add("FAN_NICKNAME");
        }

        if (selectedGenres.isEmpty()) {
            requiredSteps.add("GENRE");
        }

        if (selectedRegions.isEmpty()) {
            requiredSteps.add("REGION");
        }

        return requiredSteps;
    }

    // FAN 모드일 때만 팬 프로필(닉네임) 생성. 닉네임 필수 + 중복 검증
    private void applyFanProfile(User user, List<UserMode> selectedModes, String fanNickname) {
        if (!selectedModes.contains(UserMode.FAN)) {
            return;
        }

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

    private void saveAvailableModes(User user, List<UserMode> modes) {
        List<UserAvailableModes> entities = modes.stream()
                .distinct()
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
