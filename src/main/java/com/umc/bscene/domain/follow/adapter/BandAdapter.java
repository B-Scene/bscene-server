package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BandAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public Long countFollowersByBandId(Long bandId) {
        return followRepository.countByBand_Id(bandId);
    }

    @Override
    public boolean isFollowing(Long userId, Long bandId) {
        return followRepository.existsByBand_IdAndUser_Id(bandId, userId);
    }
}
