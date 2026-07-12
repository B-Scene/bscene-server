package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse;
import com.umc.bscene.domain.user.dto.response.ParticipationHistoryResponse.ParticipationHistoryItem;
import com.umc.bscene.domain.user.enums.HistoryYearFilter;
import com.umc.bscene.domain.user.port.PerformancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.util.List;

/**
 * 마이페이지의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * - countInterested          : 사용자가 관심 등록한 공연 수
 * - countParticipated        : 참여 완료(COMPLETED)한 공연 수 (참여 공연)
 * - findParticipationHistory : 참여 완료한 공연 목록 (연도 필터, 날짜/시간 빠른 순)
 */
@RequiredArgsConstructor
public class UserAdapter implements PerformancePort {

    private final PerformanceInterestRepository performanceInterestRepository;
    private final PerformanceParticipationRepository performanceParticipationRepository;

    @Override
    public long countInterested(Long userId) {
        return performanceInterestRepository.countInterestedByUserId(userId, PerformanceStatus.ACTIVE);
    }

    @Override
    public long countParticipated(Long userId) {
        return performanceParticipationRepository.countByUser_IdAndStatusAndPerformance_Status(
                userId, ParticipationStatus.COMPLETED, PerformanceStatus.ACTIVE);
    }

    @Override
    public ParticipationHistoryResponse findParticipationHistory(
            Long userId, HistoryYearFilter appliedFilter, int baseYear,
            LocalDate startDate, LocalDate endDate, int page, int size) {
        // 총 참여 공연 수(연도 필터 무관 전체)는 상단 "총 참여 공연 N회"용 → 첫 페이지에서만 조회, 이후 페이지는 생략
        Long totalCount = (page == 0)
                ? performanceParticipationRepository.countByUser_IdAndStatusAndPerformance_Status(
                        userId, ParticipationStatus.COMPLETED, PerformanceStatus.ACTIVE)
                : null;

        Slice<PerformanceParticipation> slice = performanceParticipationRepository.findCompletedHistory(
                userId, ParticipationStatus.COMPLETED, PerformanceStatus.ACTIVE,
                startDate, endDate, PageRequest.of(page, size));

        List<ParticipationHistoryItem> items = slice.getContent().stream()
                .map(this::toHistoryItem)
                .toList();

        return new ParticipationHistoryResponse(totalCount, appliedFilter, baseYear, items, page, slice.hasNext());
    }

    private ParticipationHistoryItem toHistoryItem(PerformanceParticipation participation) {
        Performance p = participation.getPerformance();
        return new ParticipationHistoryItem(
                p.getId(),
                p.getTitle(),
                p.getVenue(),
                p.getPerformanceDate(),
                p.getStartTime(),
                p.getPosterImageUrl(),
                participation.getStatus().name()
        );
    }
}
