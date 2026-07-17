package com.umc.bscene.domain.search.port;

import java.util.List;

/**
 * 검색 결과의 밴드 팔로우 여부 표시를 위한 포트 (adapter는 follow 도메인이 구현).
 */
public interface FollowPort {

    // 후보 밴드 id들 중 사용자가 팔로우 중인 밴드 id만 반환 (IN 쿼리 일괄 조회)
    List<Long> findFollowingBandIds(Long userId, List<Long> bandIds);
}
