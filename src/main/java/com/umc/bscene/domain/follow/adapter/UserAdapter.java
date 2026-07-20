package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.user.port.FollowPort;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마이페이지의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * 사용자가 팔로우한 밴드 수·장르 분포를 조회한다.
 */
@RequiredArgsConstructor
public class UserAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public long countFollowing(Long userId) {
        return followRepository.countByUser_Id(userId);
    }

    @Override
    public Map<Genre, Long> countFollowedBandsGroupedByGenre(Long userId) {
        return followRepository.countGroupedByGenre(userId).stream()
                .collect(Collectors.toMap(row -> (Genre) row[0], row -> (Long) row[1]));
    }
}
