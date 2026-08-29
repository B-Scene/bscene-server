package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@IncludesPendingBands(reason = "검수 플로우 저장소 - PENDING 요청을 다루는 것이 본질이다")
public interface BandCreationRequestRepository extends JpaRepository<BandCreationRequest, Long> {

    // 수락/거절 처리 전용: 아직 검수 중(PENDING)인 요청만 조회.
    // PESSIMISTIC_WRITE(FOR UPDATE)로 요청·밴드 row를 잠가 처리 트랜잭션을 직렬화한다.
    // 이미 처리된 요청은 빈 결과가 되어 운영진 동시/중복 클릭에도 한 번만 반영된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM BandCreationRequest r
            JOIN FETCH r.band b
            WHERE r.id = :requestId
              AND b.status = com.umc.bscene.domain.band.enums.BandStatus.PENDING
            """)
    Optional<BandCreationRequest> findPendingById(@Param("requestId") Long requestId);

    // 검수 진행 중(resolvedAt null)인 요청을 밴드로 조회 - PENDING 중 밴드 수정 시 Discord 카드 갱신용
    Optional<BandCreationRequest> findByBand_IdAndResolvedAtIsNull(Long bandId);

    // Discord 검수 메시지가 아직 전송되지 못한 진행 중 요청 - 재시도 스케줄러용
    List<BandCreationRequest> findAllByResolvedAtIsNullAndDiscordMessageIdIsNull();

    // 메신저 전용: 트랜잭션 없이 band 필드까지 안전하게 쓰도록 fetch join으로 조회
    @Query("SELECT r FROM BandCreationRequest r JOIN FETCH r.band WHERE r.id = :requestId")
    Optional<BandCreationRequest> findWithBandById(@Param("requestId") Long requestId);

    // 메신저 전용: 검수 진행 중 요청을 밴드로 fetch join 조회 (검수 카드 갱신용)
    @Query("""
            SELECT r FROM BandCreationRequest r
            JOIN FETCH r.band
            WHERE r.band.id = :bandId AND r.resolvedAt IS NULL
            """)
    Optional<BandCreationRequest> findWithBandByBandIdAndResolvedAtIsNull(@Param("bandId") Long bandId);

    // Discord 메시지 ID 조건부 저장 - 아직 미전송·미처리인 경우에만 기록되는 원자적 claim.
    // 커밋 훅과 재시도 스케줄러(또는 다중 인스턴스)가 겹쳐도 마지막 승자만 기록되고, 0을 반환받은 쪽은 중복 전송을 감지할 수 있다
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE BandCreationRequest r
            SET r.discordMessageId = :messageId
            WHERE r.id = :requestId
              AND r.discordMessageId IS NULL
              AND r.resolvedAt IS NULL
            """)
    int attachDiscordMessageIfUnsent(
            @Param("requestId") Long requestId,
            @Param("messageId") String messageId
    );

    // 검수 플로우로 밴드를 삭제하기 전, 그 밴드를 참조하는 과거 검수 이력의 FK 해제 (bandName 스냅샷은 유지)
    // 호출측 트랜잭션의 영속성 컨텍스트(교체 대상 Band, 현재 요청 등)를 비우면 안 되므로 clearAutomatically를 켜지 않는다
    @Modifying
    @Query("UPDATE BandCreationRequest r SET r.band = null WHERE r.band.id = :bandId")
    int detachBandReferences(@Param("bandId") Long bandId);
}
