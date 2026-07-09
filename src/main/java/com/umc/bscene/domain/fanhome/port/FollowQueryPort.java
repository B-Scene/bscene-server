package com.umc.bscene.domain.fanhome.port;

import java.util.List;

/**
 * 팬홈이 팔로우 정보를 조회하기 위한 포트 (adapter는 follow 도메인이 구현).
 */
public interface FollowQueryPort {
    /**
     * 사용자가 팔로우한 밴드 ID 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자가 팔로우한 밴드 ID 목록
     */
    List<Long> findFollowingBandIds(Long userId);
}
