package com.umc.bscene.domain.user.dto.response;

import java.util.List;

// 내 정보 조회/수정 응답 (내 정보 수정 화면 초기값 : 닉네임/관심 장르/활동 지역)
// 장르/지역은 enum code만 내려줌 (한글명 매칭은 /genres, /regions 목록 조회 응답 사용)
public record MyInfoResponse(
        String nickname,
        String profileImageUrl,                 // 팬모드 프로필 이미지
        List<String> genres,                    // 선택한 관심 장르 code (선택 순서, 최대 3개)
        List<String> regions                    // 선택한 활동 지역 code (선택 순서, 최대 2개)
) {
}
