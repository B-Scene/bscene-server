package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.session.port.BandMemberPort;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SessionAdapter implements BandMemberPort {

    private final BandMemberRepository bandMemberRepository;

    @Override
    public List<Long> getAcceptedMemberUserIds(Long bandId) {
        return bandMemberRepository
                .findByBand_IdAndStatus(bandId, BandMemberStatus.ACCEPTED)
                .stream()
                .map(bandMember -> bandMember.getUser().getId())
                .toList();
    }
}