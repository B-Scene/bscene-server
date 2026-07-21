package com.umc.bscene.domain.user.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 내 정보 수정 요청.
 * 화면의 저장하기 = 전체 값을 한 번에 저장하는 방식이므로 세 항목 모두 필수 (바뀌지 않은 값도 포함해서 전송)
 * - nickname: 팬모드 닉네임 (온보딩과 동일하게 8자 이내)
 * - genres: 관심 장르 1~3개
 * - regions: 활동 지역 1~2개
 */
public record MyInfoUpdateRequest(

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 8, message = "닉네임은 8자 이내여야 합니다.")
        String nickname,

        @NotEmpty(message = "관심 장르는 1개 이상 선택해야 합니다.")
        @Size(max = 3, message = "관심 장르는 최대 3개까지 선택할 수 있습니다.")
        List<Genre> genres,

        @NotEmpty(message = "활동 지역은 1개 이상 선택해야 합니다.")
        @Size(max = 2, message = "활동 지역은 최대 2개까지 선택할 수 있습니다.")
        List<Region> regions
) {
}
