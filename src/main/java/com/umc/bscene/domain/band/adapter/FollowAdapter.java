package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.follow.port.BandPort;
import lombok.RequiredArgsConstructor;

/**
 * 팔로우 도메인의 BandPort를 band 도메인이 구현하는 어댑터.
 * 팔로우 대상 밴드의 존재 확인과 자기 밴드 팔로우 차단(멤버십 확인)에 사용된다.
 */
@RequiredArgsConstructor
public class FollowAdapter implements BandPort {

    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;

    @Override
    public boolean existsBand(Long bandId) {
        return bandRepository.existsById(bandId);
    }

    @Override
    public boolean isAcceptedMember(Long bandId, Long userId) {
        return bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(bandId, userId, BandMemberStatus.ACCEPTED);
    }
}
