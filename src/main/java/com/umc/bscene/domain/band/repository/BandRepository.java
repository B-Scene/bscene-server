package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import org.springframework.data.domain.Pageable;
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

    // 밴드 추천 콜드스타트 폴백 보완 : 팔로우 데이터가 부족할 때 채울 최근 생성 밴드 (신생 밴드 노출 기회 제공)
    List<Band> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 사용자의 현재 활성화된 밴드 멤버 프로필이 소속된 밴드 id 조회 (수락된 정식 멤버 기준, 먼저 가입한 밴드 순)
    @Query("""
select b.id
from Band b
    inner join b.bandMembers bm
where bm.user.id = :userId
    and bm.bandMemberProfile.active = true
    and bm.memberType = :memberType
    and bm.status = :status
order by bm.id asc
""")
    List<Long> findBandIdsByActiveProfile(
            @Param("userId") Long userId,
            @Param("memberType") BandMemberType memberType,
            @Param("status") BandMemberStatus status
    );
}
