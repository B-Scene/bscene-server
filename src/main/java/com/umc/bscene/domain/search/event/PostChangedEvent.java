package com.umc.bscene.domain.search.event;

/**
 * 게시물 생성/수정/삭제(및 영상 썸네일 생성 완료) 시 발행 — 검색 색인 동기화용.
 * id만 담고 리스너가 원본(MySQL)을 다시 조회한다 (VIDEO 타입이 아니거나 삭제됐으면 문서 삭제로 처리).
 */
public record PostChangedEvent(Long postId) {
}
