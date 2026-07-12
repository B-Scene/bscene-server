package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.performance.dto.response.PerformanceAlarmResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
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
public class PerformanceAlarmService {

    private final PerformanceParticipationRepository performanceParticipationRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final PerformanceInterestService performanceInterestService;

    // 사용자가 공연 알림을 설정 → 참여 예정(SCHEDULED) 상태의 참여 기록 생성
    @Transactional
    public PerformanceAlarmResponse setAlarm(Long userId, Long performanceId) {
        // 대상 공연이 존재하는지 확인 (없거나 삭제된 경우 404 반환)
        Performance performance = getActivePerformance(performanceId);

        // 이미 참여 기록(예정/완료)이 있는 경우 중복 저장 방지
        if (performanceParticipationRepository.existsByPerformance_IdAndUser_Id(performanceId, userId)) {
            throw new PerformanceException(PerformanceErrorCode.ALREADY_ALARM_SET);
        }

        try {
            // 알림 설정 = 참여 예정 상태로 참여 기록 생성 (status는 기본값 SCHEDULED)
            performanceParticipationRepository.save(PerformanceParticipation.builder()
                    .performance(performance)
                    .user(userRepository.getReferenceById(userId))
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 위 중복 체크를 둘 다 통과한 경우, 유니크 제약이 최종 방어 → 409로 변환
            throw new PerformanceException(PerformanceErrorCode.ALREADY_ALARM_SET);
        }

        // 알림을 설정하면 관심 공연으로도 함께 등록한다. (이미 관심 등록돼 있으면 멱등하게 통과)
        performanceInterestService.ensureInterest(userId, performance);

        return PerformanceAlarmResponse.of(performanceId, true);
    }

    // 사용자가 공연 알림을 해제 → 참여 예정(SCHEDULED) 기록만 삭제 (참여완료 이력·관심 등록은 유지)
    @Transactional
    public PerformanceAlarmResponse unsetAlarm(Long userId, Long performanceId) {
        // 참여 예정 기록이 있으면 삭제, 없으면 이미 해제 상태이므로 그대로 둠(멱등 처리)
        performanceParticipationRepository.deleteByPerformance_IdAndUser_IdAndStatus(
                performanceId, userId, ParticipationStatus.SCHEDULED);

        return PerformanceAlarmResponse.of(performanceId, false);
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
