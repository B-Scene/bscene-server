package com.umc.bscene.domain.performance.port;

import java.util.List;

public interface FollowPort {

    // 공연을 등록한 밴드를 팔로우하는 활성 사용자 ID 조회
    List<Long> getFollowerUserIdsByBandId(Long bandId);
}
