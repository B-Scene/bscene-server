package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BandMemberProfileRepository extends JpaRepository<BandMemberProfile, Long> {

    // 특정 사용자의 밴드 멤버 프로필 목록 조회
    List<BandMemberProfile> findAllByUser_Id(Long userId);
}
