package com.umc.bscene.domain.user.dto.response;

// 세션 지원 최종 확정 응답
// 확정으로 생성되는 멤버 프로필은 active=false라, FE가 이 PK로 모드 전환까지 이어가야 활성 프로필이 된다
// 최종 거절이면 생성되는 프로필이 없으므로 null
public record SessionApplyConfirmResponse(
        Long bandMemberProfileId
) {
}
