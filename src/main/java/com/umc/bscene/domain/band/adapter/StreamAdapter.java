package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StreamAdapter implements BandMemberPort {

    private final BandMemberRepository bandMemberRepository;
    private final BandRepository bandRepository;

    @Override
    public List<BandInfoForGetLiveResponse> getBandNameWithBandProfileByBroadcasterId(Set<Long> broadcasterIds) {
        if (broadcasterIds.isEmpty()) {
            return List.of();
        }

        return bandMemberRepository.findWithBandByUser_IdInAndStatus(broadcasterIds, BandMemberStatus.ACCEPTED).stream()
                // 활성 프로필이 없는 유저는 쿼리 결과에서 이미 제외됨. 정상적으로는 유저당 활성 멤버십이 최대 1개지만, 데이터 정합성이 깨진 경우를 대비해 방어적으로 dedup
                .collect(Collectors.toMap(
                        member -> member.getUser().getId(),
                        member -> member,
                        (first, second) -> first
                ))
                .values().stream()
                .map(member -> {
                    Band band = member.getBand();
                    return new BandInfoForGetLiveResponse(
                            member.getUser().getId(),
                            band.getName(),
                            band.getProfileImageUrl()
                    );
                })
                .toList();
    }

    @Override
    public Map<Long, BandInfoForGetLiveResponse.BandInfo> getBandInfoByBandIds(Set<Long> bandIds) {
        if (bandIds.isEmpty()) {
            return Map.of();
        }

        return bandRepository.findAllById(bandIds).stream()
                .collect(Collectors.toMap(
                        Band::getId,
                        band -> new BandInfoForGetLiveResponse.BandInfo(band.getName(), band.getProfileImageUrl())
                ));
    }

    @Override
    public Optional<BandSummaryResponse> getBandSummaryByBroadcasterId(Long broadcasterId) {
        return findActiveBandMembership(broadcasterId)
                .filter(BandMember::isMember)
                .map(member -> {
                    Band band = member.getBand();
                    return new BandSummaryResponse(band.getId(), band.getName());
                });
    }

    @Override
    public Optional<BandSummaryResponse> getBandSummaryByBandId(Long bandId) {
        return bandRepository.findById(bandId)
                .map(band -> new BandSummaryResponse(band.getId(), band.getName()));
    }

    @Override
    public List<CoHostCandidateInfo> getCoHostCandidatesByBandId(Long bandId) {
        // 라이브가 확정한 밴드 기준 조회. 멤버십에 연결된 프로필로 조회하므로 멤버가 타 밴드 프로필로 전환해도 목록 유지
        return bandMemberRepository
                .findWithUserAndProfileByBand_IdAndStatusAndMemberType(
                        bandId,
                        BandMemberStatus.ACCEPTED,
                        BandMemberType.MEMBER
                )
                .stream()
                .map(bm -> new CoHostCandidateInfo(
                        bm.getUser().getId(),
                        bm.getId(),
                        bm.getBandMemberProfile().getId(),
                        bm.getBand().getProfileImageUrl(),
                        bm.getBandMemberProfile().getNickname(),
                        bm.getBandMemberProfile().getPart()
                ))
                .toList();
    }

    @Override
    public boolean isActiveRegularMemberOfBand(Long bandId, Long userId) {
        return findActiveBandMembership(userId)
                .filter(BandMember::isMember)
                .map(member -> member.getBand().getId().equals(bandId))
                .orElse(false);
    }

    @Override
    public List<LiveMembersResponse.LiveMemberProfileResponse> getLiveMemberProfiles(Long bandId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<Long, BandMember> memberByUserId = bandMemberRepository
                .findWithBandAndProfileByBand_IdAndUser_IdIn(bandId, userIds).stream()
                .collect(Collectors.toMap(
                        member -> member.getUser().getId(),
                        member -> member,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return userIds.stream()
                .map(memberByUserId::get)
                .filter(Objects::nonNull)
                .map(member -> {
                    Band band = member.getBand();
                    BandMemberProfile profile = member.getBandMemberProfile();
                    boolean isLeader = band.getOwner().getId().equals(member.getUser().getId());
                    return new LiveMembersResponse.LiveMemberProfileResponse(
                            band.getProfileImageUrl(),
                            profile.getNickname(),
                            band.getName(),
                            List.of(profile.getPart()),
                            isLeader
                    );
                })
                .toList();
    }

    // 송출자의 현재 활성 프로필(BandMemberProfile.active = true)에 연결된 밴드 소속 정보를 조회
    private Optional<BandMember> findActiveBandMembership(Long broadcasterId) {
        return bandMemberRepository
                .findWithBandByUser_IdInAndStatus(Set.of(broadcasterId), BandMemberStatus.ACCEPTED)
                .stream()
                .findFirst();
    }

    @Override
    public List<Long> getAcceptedMemberUserIds(Long bandId) {
        return bandMemberRepository
                .findByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED)
                .stream()
                .map(member -> member.getUser().getId())
                .distinct()
                .toList();
    }

    @Override
    public Optional<String> getBandMemberNickname(Long bandId, Long userId) {
        return bandMemberRepository.findNicknameByBandIdAndUserIdAndStatus(
                bandId,
                userId,
                BandMemberStatus.ACCEPTED
        );
    }

    @Override
    public String getBandName(Long bandId) {
        return bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(
                        BandErrorCode.BAND_NOT_FOUND
                ))
                .getName();
    }
}
