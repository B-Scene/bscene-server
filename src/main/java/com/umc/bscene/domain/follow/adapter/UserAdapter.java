package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.user.port.FollowPort;
import lombok.RequiredArgsConstructor;

/**
 * 마이페이지의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * 사용자가 팔로우한 밴드 수를 조회한다.
 */
@RequiredArgsConstructor
public class UserAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public long countFollowing(Long userId) {
        return followRepository.countByUser_Id(userId);
    }
}
