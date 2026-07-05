package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    Optional<BandMember> findByBand_IdAndUser_Id(Long bandId, Long userId);

    List<BandMember> findByBand_Id(Long bandId);
}
