package com.umc.bscene.domain.search.event;

/**
 * 공연 생성/수정/삭제 시 발행 — 검색 색인 동기화용.
 * id만 담고 리스너가 원본(MySQL)을 다시 조회한다 (ACTIVE가 아니면 문서 삭제로 처리).
 */
public record PerformanceChangedEvent(Long performanceId) {
}
