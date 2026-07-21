package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.MusicLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicLinkRepository extends JpaRepository<MusicLink, Long> {

    Optional<MusicLink> findByBand_Id(Long bandId);
}
