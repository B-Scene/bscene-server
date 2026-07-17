package com.umc.bscene.domain.search.port;

import com.umc.bscene.domain.performance.entity.Performance;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인이 공연 데이터를 조회하기 위한 포트 (adapter는 performance 도메인이 구현).
 * 모든 조회는 band를 fetch join한 상태로 반환해야 한다 (문서 변환 시 밴드명 비정규화).
 */
public interface PerformancePort {

    // 전체 색인용 : ACTIVE 공연 전체 (band fetch join)
    List<Performance> findAllActiveWithBand();

    // 단건 색인용 : ACTIVE 공연 조회 (band fetch join, 없거나 삭제됐으면 empty → 문서 삭제 처리)
    Optional<Performance> findActiveByIdWithBand(Long performanceId);

    // 연쇄 재색인용 : 특정 밴드의 ACTIVE 공연 전체 (band fetch join)
    List<Performance> findAllActiveByBandIdWithBand(Long bandId);
}
