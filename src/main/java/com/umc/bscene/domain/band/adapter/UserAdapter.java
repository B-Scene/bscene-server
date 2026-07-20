package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class UserAdapter implements BandPort {

    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final BandMemberRepository bandMemberRepository;

    @Override
    public BandMemberResponse getActiveBandMemberProfile(Long userId) {
        // 밴드 모드 사용자는 반드시 활성 멤버 프로필을 가짐 (없으면 데이터 이상)
        BandMemberProfile profile = bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

        // 활성 프로필로 활동 중인 소속 밴드 조회 - 소속 밴드가 없으면 밴드 정보는 null로 응답
        Band band = bandMemberRepository
                .findWithBandByUser_IdInAndStatus(Set.of(userId), BandMemberStatus.ACCEPTED)
                .stream()
                .findFirst()
                .map(BandMember::getBand)
                .orElse(null);

        return new BandMemberResponse(
                band == null ? null : band.getId(),
                band == null ? null : band.getProfileImageUrl(),
                profile.getNickname(),
                band == null ? null : band.getName(),
                List.of(profile.getPart().getDescription())
        );
    }
}
