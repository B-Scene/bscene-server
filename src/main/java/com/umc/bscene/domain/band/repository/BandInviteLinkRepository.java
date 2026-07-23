package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandInviteLink;
import com.umc.bscene.domain.band.enums.BandMemberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BandInviteLinkRepository
        extends JpaRepository<BandInviteLink, Long> {

    Optional<BandInviteLink> findByBand_IdAndMemberType(
            Long bandId,
            BandMemberType memberType
    );

    Optional<BandInviteLink> findByToken(String token);
}