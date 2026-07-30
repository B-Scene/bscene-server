package com.umc.bscene.domain.stream.dto.response;

/*
 * 공동 송출자 업그레이드 SSE 이벤트 payload.
 * - coHostUpgradeRequested: 송출자에게만 전송 (수락 모달용, nickname은 요청자의 밴드 멤버 닉네임)
 * - coHostUpgradeAccepted: 요청자에게만 전송 (수신 시 FE가 enterRoom을 재호출해 송출 정보를 받는다)
 */
public record CoHostUpgradeEvent(
        Long userId,
        String nickname
) {
}
