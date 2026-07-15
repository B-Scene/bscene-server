package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.search.port.FollowPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 검색의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * 검색 결과 밴드들의 팔로우 여부 표시용.
 */
@RequiredArgsConstructor
public class SearchAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public List<Long> findFollowingBandIds(Long userId, List<Long> bandIds) {
        if (bandIds.isEmpty()) return List.of();
        return followRepository.findBandIdsByUserIdAndBandIdIn(userId, bandIds);
    }
}
