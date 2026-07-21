package com.umc.bscene.domain.band.port;

public interface FollowPort {
    /**
     * 밴드의 팔로워 수를 조회합니다.
     *
     * @param bandId 팔로워 수를 조회할 밴드 ID
     * @return 해당 밴드를 팔로우한 사용자 수
     */
    Long countFollowersByBandId(Long bandId);
}