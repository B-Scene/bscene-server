package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BandMemberRepository extends JpaRepository<BandMember, Long> {

    boolean existsByBand_IdAndUser_Id(Long bandId, Long userId);

    boolean existsByBand_IdAndUser_IdAndStatus(Long bandId, Long userId, BandMemberStatus status);

    Optional<BandMember> findByBand_IdAndUser_Id(Long bandId, Long userId);

    List<BandMember> findByBand_IdOrderByIdAsc(Long bandId);

    List<BandMember> findByBand_IdAndStatus(Long bandId, BandMemberStatus status);

    List<BandMember> findBySessionApplication_SessionApplicationId(
            Long sessionApplicationId
    );

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

    // 송출자(userId) 목록에 대응하는 밴드 소속 정보를 밴드까지 한 번에 조회 (사용자당 가장 먼저 가입한 밴드 하나만 필요)
    @Query("""
        SELECT bm
        FROM BandMember bm
        JOIN FETCH bm.band
        WHERE bm.user.id IN :userIds
          AND bm.status = :status
        ORDER BY bm.id ASC
    """)
    List<BandMember> findWithBandByUser_IdInAndStatus(
            @Param("userIds") Collection<Long> userIds,
            @Param("status") BandMemberStatus status
    );
}
