package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandRepository extends JpaRepository<Band, Long> {

    boolean existsByName(String name);

    // 밴드 추천 후보군 조회 : 선호 장르가 일치하는 밴드
    List<Band> findByGenreIn(Collection<Genre> genres);

    // 밴드 추천 후보군 조회 : 선호 지역이 일치하는 밴드
    List<Band> findByRegionIn(Collection<Region> regions);

    // 사용자 기본 키 사용. 활성화된 밴드 멤버 프로필에 연관된 밴드 조회
    @Query("""
select b
from BandMember bm
    left join bm.bandMemberProfile bmp
    left join bm.band b
where bm.user.id = :userId
    and bmp.active = :isActive
    and bm.status = :status
""")
    Optional<Band> findByUserIdWithActiveProfile(
            @Param("userId") Long userId,
            @Param("isActive") Boolean isActive,
            @Param("status") BandMemberStatus status
    );
}
