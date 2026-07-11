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
public class BandMemberPortAdapter implements BandMemberPort {

    private final BandMemberRepository bandMemberRepository;

    @Override
    public List<BandInfoForGetLiveResponse> getBandNameWithBandProfileByBroadcasterId(Set<Long> broadcasterIds) {
        if (broadcasterIds.isEmpty()) {
            return List.of();
        }

        return bandMemberRepository.findWithBandByUser_IdInAndStatus(broadcasterIds, BandMemberStatus.ACCEPTED).stream()
                // 한 유저가 여러 밴드에 속해도 가장 먼저 가입한(id가 가장 작은) 밴드 하나만 사용
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
