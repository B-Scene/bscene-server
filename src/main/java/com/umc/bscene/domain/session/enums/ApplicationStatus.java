package com.umc.bscene.domain.session.enums;

public enum ApplicationStatus {
    PENDING,
    BAND_ACCEPTED,  // 밴드가 수락하여 지원자의 최종 확정(수락/거절)을 기다리는 상태
    ACCEPTED,       // 지원자까지 최종 수락하여 세션 활동이 확정된 상태
    REJECTED,
    CANCELED
}
