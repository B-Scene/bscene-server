package com.umc.bscene.domain.band.port;

/**
 * 밴드 도메인이 추천 도메인의 밴드 파생 데이터를 정리하기 위한 포트 (adapter는 recommendation 도메인이 구현).
 * 검수 거절/더미 교체로 밴드를 삭제할 때, 클릭·노출 로그·유사도 행이 FK로 남아 있으면
 * 밴드 삭제가 제약 위반으로 실패하므로 선삭제한다.
 */
public interface RecommendationPort {

    // 밴드를 참조하는 추천 데이터(클릭 상호작용, 노출 로그, 유사도 양방향) 전체 삭제
    void deleteAllByBandId(Long bandId);
}
