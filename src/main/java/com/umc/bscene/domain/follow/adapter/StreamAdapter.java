package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * stream 도메인의 FollowPort를 follow 도메인이 구현하는 어댑터.
 * - getFollowingBandIds        : 팔로우한 밴드 라이브 목록 조회용, 사용자가 팔로우한 밴드 ID 목록
 * - getFollowerUserIdsByBandId : 라이브 예약/시작 푸시 발송 대상 조회용, 밴드를 팔로우한 사용자 ID 목록
 */
@RequiredArgsConstructor
public class StreamAdapter implements FollowPort {

    private final FollowRepository followRepository;

    @Override
    public List<Long> getFollowingBandIds(Long userId) {
        return followRepository.findBandIdsByUserId(userId);
    }

    @Override
    public List<Long> getFollowerUserIdsByBandId(Long bandId) {
        // 푸시 발송 대상이므로 활성(ACTIVE) 사용자만 조회 (탈퇴·정지·휴면 제외)
        return followRepository.findUserIdsByBandId(bandId, UserStatus.ACTIVE);
    }
}
