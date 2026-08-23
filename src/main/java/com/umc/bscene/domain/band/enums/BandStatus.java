package com.umc.bscene.domain.band.enums;

/**
 * 밴드 검수 상태.
 * PENDING: 생성 요청 후 운영진 검수 대기 — 공개 조회에서 제외
 * ACCEPTED: 검수 통과 — 실제 서비스에 노출
 * 거절된 밴드는 row 자체를 삭제하므로 별도 상태가 없다 (이력은 BandCreationRequest에 남음)
 */
public enum BandStatus {
    PENDING,
    ACCEPTED
}
