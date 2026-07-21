package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.performance.port.FollowPort;
import com.umc.bscene.domain.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PerformanceAdapter implements FollowPort {

    private final FollowRepository followRepository;

    // 공연을 등록한 밴드를 팔로우하는 활성 사용자만 조회
    @Override
    public List<Long> getFollowerUserIdsByBandId(Long bandId) {
        return followRepository.findUserIdsByBandId(
                bandId,
                UserStatus.ACTIVE
        );
    }
}