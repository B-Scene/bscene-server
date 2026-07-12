package com.umc.bscene.domain.performance.adapter;

import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import com.umc.bscene.domain.user.port.PerformancePort;
import lombok.RequiredArgsConstructor;

/**
 * 마이페이지의 PerformancePort를 performance 도메인이 구현하는 어댑터.
 * - countInterested   : 사용자가 관심 등록한 공연 수
 * - countParticipated : 참여 완료(COMPLETED)한 공연 수 (참여 공연)
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
}
