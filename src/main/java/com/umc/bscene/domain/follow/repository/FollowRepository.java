package com.umc.bscene.domain.follow.repository;

import com.umc.bscene.domain.follow.dto.FollowerRow;
import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.user.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 사용자와 밴드 사이에 팔로우 관계가 존재하는지 확인
    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    // 사용자와 밴드 사이의 팔로우 관계를 삭제
    void deleteByBand_IdAndUser_Id(Long bandId, Long userId);

    // 밴드 삭제(검수 거절/더미 교체) 시 해당 밴드의 팔로우 전체 삭제
    void deleteByBand_Id(Long bandId);

    // 특정 밴드를 팔로우하는 사용자 수를 반환
    long countByBand_Id(Long bandId);

    // 특정 사용자가 팔로우하는 밴드 수를 반환
    long countByUser_Id(Long userId);

    // 특정 사용자가 팔로우하는 밴드 ID 목록을 반환
    @Query("SELECT f.band.id FROM Follow f WHERE f.user.id = :userId")
    List<Long> findBandIdsByUserId(@Param("userId") Long userId);

    // 밴드 추천 콜드스타트 폴백 : 콜드스타트에게 보여줄 팔로워 수 상위 밴드 id (검수 통과 밴드만)
    @Query("SELECT f.band.id FROM Follow f " +
            "WHERE f.band.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED " +
            "GROUP BY f.band.id ORDER BY COUNT(f) DESC")
    List<Long> findTopFollowedBandIds(Pageable pageable);

    // 특정 밴드를 팔로우하는 사용자 ID 목록을 반환 (라이브 푸시 알림 발송 대상 조회용, 활성 사용자만)
    @Query("SELECT f.user.id FROM Follow f " +
            "WHERE f.band.id = :bandId AND f.user.status = :userStatus")
    List<Long> findUserIdsByBandId(
            @Param("bandId") Long bandId,
            @Param("userStatus") UserStatus userStatus
    );

    // 후보 밴드 중 사용자가 팔로우 중인 밴드 id만 일괄 조회 (검색 결과의 팔로우 여부 표시용)
    @Query("SELECT f.band.id FROM Follow f " +
            "WHERE f.user.id = :userId AND f.band.id IN :bandIds")
    List<Long> findBandIdsByUserIdAndBandIdIn(
            @Param("userId") Long userId,
            @Param("bandIds") List<Long> bandIds
    );

    // SearchAdapter에서 사용 : 밴드별 팔로워 수 일괄 집계 (검색 인기순 popularity 색인용, [bandId, count] 행, 검수 통과 밴드만)
    @Query("SELECT f.band.id, COUNT(f) FROM Follow f " +
            "WHERE f.band.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED " +
            "GROUP BY f.band.id")
    List<Object[]> countGroupedByBand();

    // UserAdapter에서 사용 : 사용자가 팔로우한 밴드들의 장르별 밴드 수 집계 (마이페이지 대표 장르용, [genre, count] 행)
    @Query("SELECT f.band.genre, COUNT(f) FROM Follow f WHERE f.user.id = :userId GROUP BY f.band.genre")
    List<Object[]> countGroupedByGenre(@Param("userId") Long userId);

    // UserAdapter에서 사용 : 사용자가 팔로우한 밴드 목록 (마이페이지, 밴드명 가나다순, f.id로 정렬 결정성 보장)
    // 밴드 정보를 함께 쓰므로 JOIN FETCH로 N+1 방지 (band는 ManyToOne이라 페이징과 함께 안전)
    @Query("SELECT f FROM Follow f JOIN FETCH f.band b " +
            "WHERE f.user.id = :userId " +
            "AND b.status = com.umc.bscene.domain.band.enums.BandStatus.ACCEPTED " +
            "ORDER BY b.name ASC, f.id ASC")
    Slice<Follow> findFollowedBands(@Param("userId") Long userId, Pageable pageable);

    // UserAdapter / 밴드 추천(인기도 항목)에서 사용 : 조회된 밴드들의 전체 팔로워 수 일괄 집계 ([bandId, count] 행)
    @Query("SELECT f.band.id, COUNT(f) FROM Follow f WHERE f.band.id IN :bandIds GROUP BY f.band.id")
    List<Object[]> countFollowersByBandIds(@Param("bandIds") List<Long> bandIds);

    // 후보 밴드들의 최근 N일 신규 팔로워 수를 일괄 조회 (밴드ID, 증가수) - 추천 스코어의 인기도 항목용
    @Query("SELECT f.band.id, COUNT(f) FROM Follow f " +
            "WHERE f.band.id IN :bandIds AND f.createdAt >= :since GROUP BY f.band.id")
    List<Object[]> countRecentFollowersByBandIdIn(
            @Param("bandIds") List<Long> bandIds,
            @Param("since") LocalDateTime since
    );

    // BandAdapter에서 사용 : 밴드 팔로워 커서 페이징 조회 (최신 팔로우 순)
    // 팬 닉네임/이미지가 필요하므로 FanProfile을 left join (팬 프로필이 없는 사용자도 목록에서 누락되지 않게)
    @Query("""
select new com.umc.bscene.domain.follow.dto.FollowerRow(
    f.id,
    u.id,
    fp.profileImageUrl,
    fp.nickname
)
from Follow f
    join f.user u
    left join FanProfile fp on fp.user.id = u.id
where f.band.id = :bandId
    and (:cursor is null or f.id < :cursor)
order by f.id desc
""")
    List<FollowerRow> findPagedFollowers(
            @Param("bandId") Long bandId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
