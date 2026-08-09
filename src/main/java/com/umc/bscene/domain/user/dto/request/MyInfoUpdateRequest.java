package com.umc.bscene.domain.user.dto.request;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * 내 정보 수정 요청.
 * 화면의 저장하기 = 전체 값을 한 번에 저장하는 방식이므로 nickname/genres/regions는 필수 (바뀌지 않은 값도 포함해서 전송)
 * - nickname: 팬모드 닉네임 (온보딩과 동일하게 8자 이내)
 * - profileImageUrl: 팬모드 프로필 이미지 (선택) - null이면 기존 값 유지, 빈 문자열이면 이미지 삭제
 * - genres: 관심 장르 1~3개
 * - regions: 활동 지역 1~2개
 */
public record MyInfoUpdateRequest(

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 8, message = "닉네임은 8자 이내여야 합니다.")
        String nickname,

        @URL(message = "프로필 이미지는 올바른 URL 형식이어야 합니다.")
        @Size(max = 2048, message = "프로필 이미지 URL은 2048자 이하여야 합니다.")
        String profileImageUrl,

        @NotEmpty(message = "관심 장르는 1개 이상 선택해야 합니다.")
        @Size(max = 3, message = "관심 장르는 최대 3개까지 선택할 수 있습니다.")
        List<Genre> genres,

        @NotEmpty(message = "활동 지역은 1개 이상 선택해야 합니다.")
        @Size(max = 2, message = "활동 지역은 최대 2개까지 선택할 수 있습니다.")
        List<Region> regions
) {
}
