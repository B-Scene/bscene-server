package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.enums.BandStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandRepository extends JpaRepository<Band, Long> {

    // 밴드명 중복 검사 (상태별) - 생성/개명 규칙은 (name, status) 복합 유니크와 동일 기준을 쓴다
    boolean existsByNameAndStatus(String name, BandStatus status);

    // 검수 수락 시 동명의 기존 ACCEPTED 밴드(더미) 교체 대상 조회
    Optional<Band> findByNameAndStatus(String name, BandStatus status);

    // 검수 통과 밴드 전체 조회 (검색 전체 색인용)
    List<Band> findAllByStatus(BandStatus status);

    // 검수 통과 밴드 단건 조회 (검색 단건 색인 게이트용 — PENDING이면 empty)
    Optional<Band> findByIdAndStatus(Long id, BandStatus status);

    // 검수 통과 밴드 존재 확인 (팔로우 대상 검증 등 공개 경로용)
    boolean existsByIdAndStatus(Long id, BandStatus status);

    // 검수 통과 밴드만 id 목록으로 조회 (추천 후보 하이드레이션용)
    List<Band> findAllByIdInAndStatus(Collection<Long> ids, BandStatus status);

    // 밴드 추천 후보군 조회 : 선호 장르가 일치하는 검수 통과 밴드
    @Query("""
select b
from Band b
where b.genre in :genres
    and b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED
""")
    List<Band> findByGenreIn(@Param("genres") Collection<Genre> genres);

    // 밴드 추천 후보군 조회 : 선호 지역이 일치하는 검수 통과 밴드
    @Query("""
select b
from Band b
where b.region in :regions
    and b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED
""")
    List<Band> findByRegionIn(@Param("regions") Collection<Region> regions);

    // 밴드 추천 콜드스타트 폴백 보완 : 팔로우 데이터가 부족할 때 채울 최근 생성 밴드 (신생 밴드 노출 기회 제공)
    @Query("""
select b
from Band b
where b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED
order by b.createdAt desc
""")
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
    @IncludesPendingBands(reason = "소속 멤버 관점 조회(내 밴드 목록) - PENDING 밴드도 포함해야 한다")
    List<Long> findBandIdsByActiveProfile(
            @Param("userId") Long userId,
            @Param("memberType") BandMemberType memberType,
            @Param("status") BandMemberStatus status
    );
}
