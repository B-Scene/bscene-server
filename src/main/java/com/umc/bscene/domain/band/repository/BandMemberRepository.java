package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.user.dto.response.MyBandProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    // userId를 기반으로 BandMember를 조회, 관련 있는 Band를 dto projection
    @Query("""
select new com.umc.bscene.domain.user.dto.response.MyBandProfile(
b.id,
bm.bandMemberProfile.id,
b.profileImageUrl,
b.name,
b.genre,
b.region,
bm.bandMemberProfile.active
)
from BandMember bm
join bm.band b
where bm.user.id = :userId
    and bm.status = :status
order by bm.id ASC
""")
    List<MyBandProfile> getMyBandProfiles(
            @Param("userId") Long userId,
            @Param("status") BandMemberStatus status
    );

    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    boolean existsByBand_IdAndUser_IdAndStatus(Long bandId, Long userId, BandMemberStatus status);

    Optional<BandMember> findByBand_IdAndUser_Id(Long bandId, Long userId);

    // 밴드 멤버 목록 조회용 - part 조회 시 N+1을 피하기 위해 bandMemberProfile을 함께 fetch
    // 초대 대기 중인 멤버는 bandMemberProfile이 없을 수 있으므로 LEFT JOIN FETCH 사용
    @Query("""
        SELECT bm
        FROM BandMember bm
        LEFT JOIN FETCH bm.bandMemberProfile
        WHERE bm.band.id = :bandId
        ORDER BY bm.id ASC
    """)
    List<BandMember> findWithProfileByBand_IdOrderByIdAsc(@Param("bandId") Long bandId);

    List<BandMember> findByBand_IdAndStatus(Long bandId, BandMemberStatus status);

    Long countByBand_IdAndStatus(Long bandId, BandMemberStatus status);

    Optional<BandMember> findFirstByUser_IdAndStatus(Long userId, BandMemberStatus status);

    Optional<BandMember> findByIdAndUser_IdAndStatus(
            Long bandMemberId,
            Long userId,
            BandMemberStatus status
    );

    Optional<BandMember> findByBand_IdAndUser_IdAndStatus(
            Long bandId,
            Long userId,
            BandMemberStatus status
    );

    // 멤버 프로필 삭제 전, 이미 밴드에서 사용 중인지 확인
    boolean existsByBandMemberProfile_Id(Long bandMemberProfileId);

    // 송출자(userId) 목록에 대응하는 밴드 소속 정보를 밴드까지 한 번에 조회
    // 활성 프로필이 없으면 해당 유저는 결과에서 제외
    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band
        JOIN bm.bandMemberProfile bmp
        WHERE bm.user.id IN :userIds
          AND bm.status = :status
          AND bmp.active = true
    """)
    List<BandMember> findWithBandByUser_IdInAndStatus(
            @Param("userIds") Collection<Long> userIds,
            @Param("status") BandMemberStatus status
    );

    // 공동 진행 후보 조회용 - 밴드의 정회원을 밴드(이미지)·유저·밴드 멤버 프로필까지 한 번에 조회
    // 멤버 프로필이 지정되지 않은 멤버(JOIN FETCH bm.bandMemberProfile)는 자동으로 제외됨
    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band
        JOIN FETCH bm.user
        JOIN FETCH bm.bandMemberProfile
        WHERE bm.band.id = :bandId
          AND bm.status = :status
          AND bm.memberType = :memberType
    """)
    List<BandMember> findWithUserAndProfileByBand_IdAndStatusAndMemberType(
            @Param("bandId") Long bandId,
            @Param("status") BandMemberStatus status,
            @Param("memberType") BandMemberType memberType
    );

    // 라이브 방 멤버 프로필 조회용 - 라이브 생성 시점에 확정된 밴드 기준으로 멤버 프로필까지 한 번에 조회
    // 밴드 멤버 프로필이 지정되지 않은 멤버(JOIN FETCH bm.bandMemberProfile)는 자동으로 제외됨
    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band
        JOIN FETCH bm.bandMemberProfile
        WHERE bm.band.id = :bandId
          AND bm.user.id IN :userIds
    """)
    List<BandMember> findWithBandAndProfileByBand_IdAndUser_IdIn(
            @Param("bandId") Long bandId,
            @Param("userIds") Collection<Long> userIds
    );

    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band band
        JOIN FETCH band.owner
        JOIN FETCH bm.user
        JOIN FETCH bm.bandMemberProfile
        WHERE band.id = :bandId
          AND bm.status = :status
          AND bm.memberType = :memberType
        ORDER BY bm.id ASC
    """)
    List<BandMember> findPublicBandMembers(
            @Param("bandId") Long bandId,
            @Param("status") BandMemberStatus status,
            @Param("memberType") BandMemberType memberType
    );

    @Query("""
select bm
from BandMember bm
    join fetch bm.band
    join bm.bandMemberProfile bmp
where bm.user.id = :userId
    and bmp.active = :isActive
    and bm.status = :status
""")
    Optional<BandMember> findWithBandByUser_IdAndActiveProfile(
            @Param("userId") Long userId,
            @Param("isActive") Boolean isActive,
            @Param("status") BandMemberStatus status
    );

    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.user
        WHERE bm.band.id = :bandId
          AND bm.user.id IN :userIds
    """)
    List<BandMember> findWithUserByBandIdAndUserIdIn(
            @Param("bandId") Long bandId,
            @Param("userIds") Collection<Long> userIds
    );

    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band
        WHERE bm.id IN :bandMemberIds
          AND bm.user.id = :userId
    """)
    List<BandMember> findInviteDetails(
            @Param("userId") Long userId,
            @Param("bandMemberIds") Collection<Long> bandMemberIds
    );

    @Query("""
        SELECT bm.band.id, COUNT(bm.id)
        FROM BandMember bm
        WHERE bm.band.id IN :bandIds
          AND bm.status = :status
        GROUP BY bm.band.id
    """)
    List<Object[]> countMembersByBandIdsAndStatus(
            @Param("bandIds") Collection<Long> bandIds,
            @Param("status") BandMemberStatus status
    );

    @Query("""
        SELECT bm.user.id
        FROM BandMember bm
        WHERE bm.band.id = :bandId
          AND bm.status = :status
        ORDER BY bm.id ASC
    """)
    List<Long> findUserIdsByBandIdAndStatus(
            @Param("bandId") Long bandId,
            @Param("status") BandMemberStatus status
    );

    @Query("""
    SELECT bmp.nickname
    FROM BandMember bm
    JOIN bm.bandMemberProfile bmp
    WHERE bm.band.id = :bandId
      AND bm.user.id = :userId
      AND bm.status = :status
""")
    Optional<String> findNicknameByBandIdAndUserIdAndStatus(
            @Param("bandId") Long bandId,
            @Param("userId") Long userId,
            @Param("status") BandMemberStatus status
    );
}
