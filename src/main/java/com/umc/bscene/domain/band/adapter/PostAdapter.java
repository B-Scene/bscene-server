package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.post.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * 게시물 도메인의 BandPort를 band 도메인이 구현하는 어댑터.
 * 밴드모드 댓글의 자격(멤버십) 확인과 명의(그 밴드 멤버십에 연결된 프로필) 조회에 사용된다.
 */
@RequiredArgsConstructor
public class PostAdapter implements BandPort {

    private final BandMemberRepository bandMemberRepository;

    @Override
    public boolean isAcceptedMember(Long bandId, Long userId) {
        return bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(bandId, userId, BandMemberStatus.ACCEPTED);
    }

    @Override
    public Optional<Long> findMemberProfileId(Long bandId, Long userId) {
        return bandMemberRepository.findByBand_IdAndUser_IdAndStatus(bandId, userId, BandMemberStatus.ACCEPTED)
                .map(BandMember::getBandMemberProfile)
                .map(BandMemberProfile::getId);
    }
}
