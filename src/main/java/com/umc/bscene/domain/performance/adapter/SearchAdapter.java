package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.search.port.PerformancePort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * 색인 대상은 ACTIVE 공연만 — 소프트 삭제된 공연은 조회에서 제외되고, 검색 쪽에서는 문서 삭제로 처리한다.
 */
@RequiredArgsConstructor
public class SearchAdapter implements PerformancePort {

    private final PerformanceRepository performanceRepository;

    @Override
    public List<Performance> findAllActiveWithBand() {
        return performanceRepository.findAllByStatusWithBand(PerformanceStatus.ACTIVE);
    }

    @Override
    public Optional<Performance> findActiveByIdWithBand(Long performanceId) {
        return performanceRepository.findByIdAndStatusWithBand(performanceId, PerformanceStatus.ACTIVE);
    }

    @Override
    public List<Performance> findAllActiveByBandIdWithBand(Long bandId) {
        return performanceRepository.findAllByBandIdAndStatusWithBand(bandId, PerformanceStatus.ACTIVE);
    }
}
