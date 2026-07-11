package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.port.PerformancePort;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 팬홈의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * - findUpcomingByBandIds : 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연 (시작 일시 가까운 순)
 * - recommendPerformances : 관심 등록 수가 많은 순으로 조회하는 추천 공연
 */
@RequiredArgsConstructor
public class FanHomeAdapter implements PerformancePort {

    private final PerformanceRepository performanceRepository;

    @Override
    public List<HomePerformanceItem> findUpcomingByBandIds(List<Long> bandIds, int limit) {
        if (bandIds.isEmpty()) {
            return List.of();
        }
        return performanceRepository.findUpcomingByBandIds(
                        bandIds,
                        PerformanceStatus.ACTIVE,
                        LocalDate.now(),
                        LocalTime.now(),
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    public List<HomePerformanceItem> recommendPerformances(int limit) {
        return performanceRepository.findPopularByInterest(
                        PerformanceStatus.ACTIVE,
                        LocalDate.now(),
                        LocalTime.now(),
                        PageRequest.of(0, limit)
                ).stream()
                .map(this::toItem)
                .toList();
    }

    private HomePerformanceItem toItem(Performance p) {
        return new HomePerformanceItem(
                p.getId(),
                p.getTitle(),
                p.getVenue(),
                p.getPerformanceDate(),
                p.getStartTime(),
                p.getPosterImageUrl()
        );
    }
}
