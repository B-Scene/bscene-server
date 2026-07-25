package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.notification.dto.response.BandInviteNotificationDetailResponse;
import com.umc.bscene.domain.notification.port.BandInvitePort;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BandInviteNotificationAdapter implements BandInvitePort {

    private final BandMemberRepository bandMemberRepository;

    @Override
    public Map<Long, BandInviteNotificationDetailResponse> getBandInviteDetails(Long userId, Collection<Long> bandMemberIds) {
        if (bandMemberIds.isEmpty()) {
            return Map.of();
        }

        List<BandMember> bandMembers = bandMemberRepository.findInviteDetails(userId, bandMemberIds);

        if (bandMembers.isEmpty()) {
            return Map.of();
        }

        Set<Long> bandIds = bandMembers.stream()
                .map(bandMember -> bandMember.getBand().getId())
                .collect(Collectors.toSet());

        Map<Long, Long> acceptedMemberCountByBandId = new HashMap<>();

        for (Object[] row : bandMemberRepository.countMembersByBandIdsAndStatus(bandIds, BandMemberStatus.ACCEPTED)) {
            acceptedMemberCountByBandId.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, BandInviteNotificationDetailResponse> details = new HashMap<>();

        for (BandMember bandMember : bandMembers) {
            Band band = bandMember.getBand();

            BandInviteNotificationDetailResponse detail = new BandInviteNotificationDetailResponse(
                    bandMember.getId(),
                    band.getId(),
                    band.getName(),
                    band.getProfileImageUrl(),
                    band.getGenre(),
                    band.getRegion(),
                    acceptedMemberCountByBandId.getOrDefault(band.getId(), 0L),
                    bandMember.getMemberType(),
                    bandMember.getStatus(),
                    bandMember.getStatus() == BandMemberStatus.INVITED
            );

            details.put(bandMember.getId(), detail);
        }

        return details;
    }
}