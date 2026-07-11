package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StreamAdapter implements BandMemberPort {

    private final BandMemberRepository bandMemberRepository;

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
}
