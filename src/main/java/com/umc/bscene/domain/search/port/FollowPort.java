package com.umc.bscene.domain.search.port;

import java.util.List;
import java.util.Map;

/**
 * 검색이 팔로우 데이터를 조회하기 위한 포트 (adapter는 follow 도메인이 구현).
 * 팔로우 여부 표시(검색 응답)와 인기순 popularity 집계(색인)에 사용.
 */
public interface FollowPort {

    // 후보 밴드 id들 중 사용자가 팔로우 중인 밴드 id만 반환 (IN 쿼리 일괄 조회)
    List<Long> findFollowingBandIds(Long userId, List<Long> bandIds);

    // 전체 색인용 : 밴드별 팔로워 수 일괄 집계 (GROUP BY 한 번 — 밴드별 COUNT N번 방지)
    Map<Long, Long> countFollowersGroupedByBand();

    // 단건 색인용 : 특정 밴드의 팔로워 수
    long countFollowers(Long bandId);
}
