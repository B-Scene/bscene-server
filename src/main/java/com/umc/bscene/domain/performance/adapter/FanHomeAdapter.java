package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.HomePerformanceItem;
import com.umc.bscene.domain.fanhome.dto.response.PerformanceListItem;
import com.umc.bscene.domain.fanhome.dto.response.UpcomingPerformanceResponse;
import com.umc.bscene.domain.fanhome.enums.UpcomingSortType;
import com.umc.bscene.domain.fanhome.port.PerformancePort;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 팬홈의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * - findUpcomingByBandIds : 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연 (시작 일시 가까운 순)
 * - recommendPerformances : 관심 등록 수가 많은 순으로 조회하는 추천 공연
 */
@RequiredArgsConstructor
public class FanHomeAdapter implements PerformancePort {

    private final PerformanceRepository performanceRepository;
    private final PerformanceInterestRepository performanceInterestRepository;

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

    @Override
    public UpcomingPerformanceResponse findUpcoming(Long userId, List<Long> bandIds, UpcomingSortType sort, int page, int size) {
        if (bandIds.isEmpty()) {
            return new UpcomingPerformanceResponse(sort, List.of(), page, false);
        }

        PageRequest pageable = PageRequest.of(page, size);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Slice<Performance> slice = switch (sort) {
            case IMMINENT -> performanceRepository.findUpcomingImminent(bandIds, PerformanceStatus.ACTIVE, today, now, pageable);
            case LATEST -> performanceRepository.findUpcomingLatest(bandIds, PerformanceStatus.ACTIVE, today, now, pageable);
            case POPULAR -> performanceRepository.findUpcomingPopular(bandIds, PerformanceStatus.ACTIVE, today, now, pageable);
        };

        List<PerformanceListItem> items = toListItems(userId, slice.getContent());
        return new UpcomingPerformanceResponse(sort, items, page, slice.hasNext());
    }

    // 공연 목록 → 로그인 유저의 관심 등록 여부를 일괄 조회해 아이템으로 변환
    private List<PerformanceListItem> toListItems(Long userId, List<Performance> performances) {
        if (performances.isEmpty()) {
            return List.of();
        }
        List<Long> performanceIds = performances.stream().map(Performance::getId).toList();
        Set<Long> interestedIds = Set.copyOf(
                performanceInterestRepository.findInterestedPerformanceIds(userId, performanceIds));
        return performances.stream()
                .map(p -> toListItem(p, interestedIds.contains(p.getId())))
                .toList();
    }

    private PerformanceListItem toListItem(Performance p, boolean isInterested) {
        return new PerformanceListItem(
                p.getId(),
                p.getTitle(),
                p.getVenue(),
                p.getPerformanceDate(),
                p.getStartTime(),
                p.getPosterImageUrl(),
                isInterested
        );
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
