package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.performance.dto.response.PerformanceInterestResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceInterest;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceInterestService {

    private final PerformanceInterestRepository performanceInterestRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    // 사용자가 공연을 관심 등록
    @Transactional
    public PerformanceInterestResponse setInterest(Long userId, Long performanceId) {
        // 관심 등록 대상 공연이 존재하는지 확인 (없거나 삭제된 경우 404 반환)
        Performance performance = getActivePerformance(performanceId);

        // 이미 관심 등록한 경우 중복 저장 방지
        if (performanceInterestRepository.existsByPerformance_IdAndUser_Id(performanceId, userId)) {
            throw new PerformanceException(PerformanceErrorCode.ALREADY_INTEREST_SET);
        }

        try {
            performanceInterestRepository.save(PerformanceInterest.builder()
                    .performance(performance)
                    .user(userRepository.getReferenceById(userId))
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 위 중복 체크를 둘 다 통과한 경우, 유니크 제약이 최종 방어 → 409로 변환
            throw new PerformanceException(PerformanceErrorCode.ALREADY_INTEREST_SET);
        }

        return PerformanceInterestResponse.of(performanceId, true);
    }

    // 관심 등록이 없으면 등록하고, 이미 있으면 그대로 둔다(멱등).
    // 공연 알림 설정 시 함께 관심 공연으로 등록하기 위해 호출된다. (이미 관심 등록된 경우 예외 없이 통과)
    @Transactional
    public void ensureInterest(Long userId, Performance performance) {
        if (performanceInterestRepository.existsByPerformance_IdAndUser_Id(performance.getId(), userId)) {
            return;
        }

        performanceInterestRepository.save(PerformanceInterest.builder()
                .performance(performance)
                .user(userRepository.getReferenceById(userId))
                .build());
    }

    // 사용자가 공연 관심 등록을 해제
    @Transactional
    public PerformanceInterestResponse unsetInterest(Long userId, Long performanceId) {
        // 관심 등록이 있으면 삭제, 없으면 이미 해제 상태이므로 그대로 둠(멱등 처리)
        performanceInterestRepository.deleteByPerformance_IdAndUser_Id(performanceId, userId);

        return PerformanceInterestResponse.of(performanceId, false);
    }

    // TODO : PerformanceService와 중복되는 로직. 통합 고려
    private Performance getActivePerformance(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new PerformanceException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        if (performance.getStatus() != PerformanceStatus.ACTIVE) {
            throw new PerformanceException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return performance;
    }
}
