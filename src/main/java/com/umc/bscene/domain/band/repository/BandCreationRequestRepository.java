package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandCreationRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}
