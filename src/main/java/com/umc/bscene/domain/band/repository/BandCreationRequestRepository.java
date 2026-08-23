package com.umc.bscene.domain.band.repository;

import com.umc.bscene.domain.band.entity.BandCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BandCreationRequestRepository extends JpaRepository<BandCreationRequest, Long> {

    // 수락/거절 처리 전용: 아직 검수 중(PENDING)인 요청만 조회.
    // 이미 처리된 요청은 빈 결과가 되어 운영진 동시/중복 클릭에도 한 번만 반영된다.
    @Query("""
            SELECT r FROM BandCreationRequest r
            JOIN FETCH r.band b
            WHERE r.id = :requestId
              AND b.status = com.umc.bscene.domain.band.enums.BandStatus.PENDING
            """)
    Optional<BandCreationRequest> findPendingById(@Param("requestId") Long requestId);
}
