package com.umc.bscene.domain.search.event;

/**
 * 밴드 생성/수정 시 발행 — 검색 색인 동기화용.
 * id만 담고 리스너가 원본(MySQL)을 다시 조회한다 (조회 결과 없으면 문서 삭제로 처리).
 */
public record BandChangedEvent(Long bandId) {
}
