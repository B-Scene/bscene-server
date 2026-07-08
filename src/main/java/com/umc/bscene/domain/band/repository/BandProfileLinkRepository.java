package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BandProfileLinkRepository extends JpaRepository<BandProfileLink, Long> {

    List<BandProfileLink> findAllByBandProfileBandProfileIdAndDeletedAtIsNull(
            Long bandProfileId
    );
}