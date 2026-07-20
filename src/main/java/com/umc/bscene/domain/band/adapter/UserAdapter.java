package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.SessionPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.dto.response.MyBandProfile;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.port.BandPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class UserAdapter implements BandPort {

    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final BandMemberRepository bandMemberRepository;
    private final FollowPort followPort;
    private final SessionPort sessionPort;
    private final PerformancePort performancePort;

    @Override
    public BandMemberResponse getActiveBandMemberProfile(Long userId) {
        // 밴드 모드 사용자는 반드시 활성 멤버 프로필을 가짐 (없으면 데이터 이상)
        BandMemberProfile profile = bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

        // 활성 프로필로 활동 중인 소속 밴드 조회 - 소속 밴드가 없으면 밴드 정보는 null로 응답
        BandMember bandMember = bandMemberRepository
                .findWithBandByUser_IdInAndStatus(Set.of(userId), BandMemberStatus.ACCEPTED)
                .stream()
                .findFirst()
                .orElse(null);

        assert bandMember != null;
        Band band = bandMember.getBand();

        return buildBandMemberResponse(band, profile, bandMember.isMember());
    }

    @Override
    public List<MyBandProfile> getAssociatedBandProfiles(Long userId) {
        bandMemberRepository.getMyBandProfiles(userId, BandMemberStatus.ACCEPTED);
        return List.of();
    }

    private BandMemberResponse buildBandMemberResponse(Band band, BandMemberProfile profile, boolean isMember) {
        if (band != null) {
            return new BandMemberResponse(
                    band.getProfileImageUrl(),
                    profile.getNickname(),
                    band.getName(),
                    List.of(profile.getPart().getDescription()),
                    Math.toIntExact(followPort.countFollowersByBandId(band.getId())),
                    Math.toIntExact(sessionPort.getActiveSessionApplicantCount(band.getId())),
                    Math.toIntExact(performancePort.countPerformancesByBandIdAndStatus(band.getId())),
                    isMember
            );
        } else {
            return new BandMemberResponse(
                    null,
                    profile.getNickname(),
                    null,
                    List.of(profile.getPart().getDescription()),
                    null,
                    null,
                    null,
                    isMember
            );
        }
    }
}
