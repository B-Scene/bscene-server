package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BandMemberProfileRepository extends JpaRepository<BandMemberProfile, Long> {

    // 특정 사용자의 밴드 멤버 프로필 목록 조회
    List<BandMemberProfile> findAllByUser_Id(Long userId);

    // 본인 소유의 멤버 프로필만 단건 조회 (조회/수정/삭제 시 소유권 검증)
    Optional<BandMemberProfile> findByIdAndUser_Id(Long id, Long userId);
}
