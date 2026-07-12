package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerformanceParticipationRepository extends JpaRepository<PerformanceParticipation, Long> {

    // 사용자와 공연 사이에 참여 기록(알림)이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 참여 기록 조회 (참여완료 처리용)
    Optional<PerformanceParticipation> findByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 특정 상태 참여 기록을 삭제 (알림 해제는 SCHEDULED만 삭제 → 참여완료 이력 보존)
    void deleteByPerformance_IdAndUser_IdAndStatus(Long performanceId, Long userId, ParticipationStatus status);

    // 마이페이지 참여 공연 수 : 참여 완료(COMPLETED) 상태 공연 수 (삭제된 공연 제외)
    long countByUser_IdAndStatusAndPerformance_Status(
            Long userId, ParticipationStatus status, PerformanceStatus performanceStatus);
}
