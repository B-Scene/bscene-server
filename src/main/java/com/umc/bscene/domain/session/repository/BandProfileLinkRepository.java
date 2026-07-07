package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.BandProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BandProfileLinkRepository extends JpaRepository<BandProfileLink, Long> {

    List<BandProfileLink> findAllBySessionProfileSessionProfileIdAndDeletedAtIsNull(
            Long sessionProfileId
    );
}