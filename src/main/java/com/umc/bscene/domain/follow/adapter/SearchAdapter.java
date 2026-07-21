package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.search.port.FollowPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 검색의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * 검색 결과 밴드의 팔로우 여부 표시 + 인기순 popularity(팔로워 수) 집계용.
 */
@RequiredArgsConstructor
public class SearchAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public List<Long> findFollowingBandIds(Long userId, List<Long> bandIds) {
        if (bandIds.isEmpty()) return List.of();
        return followRepository.findBandIdsByUserIdAndBandIdIn(userId, bandIds);
    }

    @Override
    public Map<Long, Long> countFollowersGroupedByBand() {
        return followRepository.countGroupedByBand().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    public long countFollowers(Long bandId) {
        return followRepository.countByBand_Id(bandId);
    }
}
