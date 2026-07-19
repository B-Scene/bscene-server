package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.search.port.PerformancePort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 검색 색인의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * 색인 대상은 ACTIVE 공연만 — 소프트 삭제된 공연은 조회에서 제외되고, 검색 쪽에서는 문서 삭제로 처리한다.
 * 인기순 popularity(관심 등록 수) 집계도 담당.
 */
@RequiredArgsConstructor
public class SearchAdapter implements PerformancePort {

    private final PerformanceRepository performanceRepository;
    private final PerformanceInterestRepository performanceInterestRepository;

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

    @Override
    public Map<Long, Long> countInterestsGroupedByPerformance() {
        return toCountMap(performanceInterestRepository.countGroupedByPerformance());
    }

    @Override
    public Map<Long, Long> countInterestsByPerformanceIds(List<Long> performanceIds) {
        if (performanceIds.isEmpty()) return Map.of();    // IN () 는 JPQL 문법 오류라 방어
        return toCountMap(performanceInterestRepository.countGroupedByPerformanceIds(performanceIds));
    }

    @Override
    public long countInterests(Long performanceId) {
        return performanceInterestRepository.countByPerformance_Id(performanceId);
    }

    // GROUP BY 결과([performanceId, count] 행)를 Map으로 변환
    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
