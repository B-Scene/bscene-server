package com.umc.bscene.domain.user.port;

/**
 * 마이페이지가 팔로우 정보를 조회하기 위한 포트 (adapter는 follow 도메인이 구현).
 */
public interface FollowPort {

    /**
     * 사용자가 팔로우한 밴드 수를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 팔로우한 밴드 수
     */
    long countFollowing(Long userId);
}
