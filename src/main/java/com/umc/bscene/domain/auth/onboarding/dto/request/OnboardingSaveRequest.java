package com.umc.bscene.domain.auth.onboarding.dto.request;

import com.umc.bscene.domain.auth.onboarding.enums.Genre;
import com.umc.bscene.domain.auth.onboarding.enums.Region;
import com.umc.bscene.domain.user.enums.UserMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 온보딩 정보 저장 요청.
 * - selectedModes: 사용 가능 모드 (팬으로 시작=[FAN], 밴드로 시작=[BAND], 둘 다=[FAN, BAND])
 * - initialMode: 온보딩 완료 후 진입할 현재 활성 모드 (selectedModes 중 하나)
 * - fanNickname: selectedModes에 FAN이 포함될 때 필수 (팬모드 닉네임)
 * - genres: 관심 장르 1~3개
 * - regions: 활동 지역 1~2개
 * (약관 동의는 회원가입 단계에서 처리 → 온보딩에서는 받지 않음)
 */
public record OnboardingSaveRequest(

        @NotEmpty
        List<UserMode> selectedModes,

        @NotNull
        UserMode initialMode,

        @Size(max = 8, message = "닉네임은 8자 이내여야 합니다.")
        String fanNickname,

        @NotEmpty
        @Size(max = 3, message = "관심 장르는 최대 3개까지 선택할 수 있습니다.")
        List<Genre> genres,

        @NotEmpty
        @Size(max = 2, message = "활동 지역은 최대 2개까지 선택할 수 있습니다.")
        List<Region> regions
) {
}
