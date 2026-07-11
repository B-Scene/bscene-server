package com.umc.bscene.domain.stream.dto.response;

// 라이브 홈 통합 응답의 추상 타입. currentMode에 따라 팬/밴드 구현체 중 하나가 내려간다
public sealed interface LiveHomeResponse permits FanLiveHomeResponse, BandLiveHomeResponse {
}
