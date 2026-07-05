package com.umc.bscene.domain.auth.onboarding.service;

import com.umc.bscene.domain.auth.onboarding.dto.response.FanNicknameCheckResponse;
import com.umc.bscene.domain.auth.onboarding.dto.response.GenreResponse;
import com.umc.bscene.domain.auth.onboarding.dto.response.OnboardingStatusResponse;
import com.umc.bscene.domain.auth.onboarding.dto.response.RegionResponse;
import com.umc.bscene.domain.auth.onboarding.enums.Genre;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.entity.UserAvailableModes;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserAvailableModesRepository;
import com.umc.bscene.domain.user.repository.UserGenresRepository;
import com.umc.bscene.domain.user.repository.UserRegionsRepository;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

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

    // 내 온보딩 상태 조회
    public OnboardingStatusResponse getMyOnboardingStatus(AuthMember authMember) {
        User user = authMember.getUser();

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

    // 팬 닉네임 중복 확인
    public FanNicknameCheckResponse checkFanNickname(String nickname) {
        boolean exists = fanProfileRepository.existsByNickname(nickname);

        return new FanNicknameCheckResponse(!exists);
    }
}