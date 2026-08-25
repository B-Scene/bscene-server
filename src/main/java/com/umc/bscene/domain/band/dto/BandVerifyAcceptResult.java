package com.umc.bscene.domain.band.dto;

import java.time.LocalDateTime;

/**
 * 검수 수락 처리 결과.
 * NEEDS_REPLACE_CONFIRM은 수락 시 삭제·교체될 동명 ACCEPTED 밴드가 있어
 * 운영진의 명시적 확인이 필요한 상태 — 교체 여부 판정이 accept() 트랜잭션(비관적 락) 안에서
 * 이루어지므로, 별도 사전 조회 방식과 달리 확인 시점과 판정 시점이 어긋나지 않는다.
 */
public record BandVerifyAcceptResult(
        Outcome outcome,
        Long bandId,
        ReplaceTarget replaceTarget
) {

    public enum Outcome {
        ACCEPTED,
        ALREADY_PROCESSED,
        NEEDS_REPLACE_CONFIRM
    }

    // 교체(삭제) 대상 기존 밴드 정보 - 운영진이 더미인지 실제 활동 밴드인지 판단할 근거
    public record ReplaceTarget(
            Long bandId,
            String bandName,
            long memberCount,
            long followerCount,
            LocalDateTime createdAt
    ) {
    }

    public static BandVerifyAcceptResult accepted(Long bandId) {
        return new BandVerifyAcceptResult(Outcome.ACCEPTED, bandId, null);
    }

    public static BandVerifyAcceptResult alreadyProcessed() {
        return new BandVerifyAcceptResult(Outcome.ALREADY_PROCESSED, null, null);
    }

    public static BandVerifyAcceptResult needsReplaceConfirm(ReplaceTarget replaceTarget) {
        return new BandVerifyAcceptResult(Outcome.NEEDS_REPLACE_CONFIRM, null, replaceTarget);
    }
}
