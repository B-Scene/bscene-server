package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.fanhome.port.FollowPort;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class FanHomeAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public List<Long> findFollowingBandIds(Long userId) {
        return followRepository.findBandIdsByUserId(userId);
    }
}
