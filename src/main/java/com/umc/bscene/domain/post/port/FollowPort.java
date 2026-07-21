package com.umc.bscene.domain.post.port;

import java.util.List;

public interface FollowPort {

    // 게시물을 등록한 밴드를 팔로우하는 활성 사용자 ID 조회
    List<Long> getFollowerUserIdsByBandId(Long bandId);
}