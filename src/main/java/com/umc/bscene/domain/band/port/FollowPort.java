package com.umc.bscene.domain.band.port;

public interface FollowPort {
    /**
     * 밴드의 팔로워 수를 조회합니다.
     *
     * @param bandId 팔로워 수를 조회할 밴드 ID
     * @return 해당 밴드를 팔로우한 사용자 수
     */
    Long countFollowersByBandId(Long bandId);

    /**
     * 사용자가 밴드를 팔로우 중인지 확인합니다. (팬모드 밴드 상세의 팔로우 버튼 상태용)
     *
     * @param userId 확인할 사용자 ID
     * @param bandId 확인할 밴드 ID
     * @return 팔로우 중이면 true
     */
    boolean isFollowing(Long userId, Long bandId);
}