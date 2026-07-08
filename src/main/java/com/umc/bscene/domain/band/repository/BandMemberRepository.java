package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    boolean existsByBand_IdAndUser_IdAndStatus(Long bandId, Long userId, BandMemberStatus status);

    Optional<BandMember> findByBand_IdAndUser_Id(Long bandId, Long userId);

    List<BandMember> findByBand_IdOrderByIdAsc(Long bandId);
}
