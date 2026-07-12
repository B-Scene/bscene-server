package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface PerformanceParticipationRepository extends JpaRepository<PerformanceParticipation, Long> {

    // 사용자와 공연 사이에 참여 기록(알림)이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 특정 상태 참여 기록을 삭제 (알림 해제는 SCHEDULED만 삭제 → 참여완료 이력 보존)
    void deleteByPerformance_IdAndUser_IdAndStatus(Long performanceId, Long userId, ParticipationStatus status);

    // 마이페이지 참여 공연 수 : 알림 신청한 공연 중 시작 시각이 지난 공연 수 (삭제된 공연 제외)
    // TODO : 참여완료 기능 도입 후 status = COMPLETED 기준으로 변경 예정
    @Query("SELECT COUNT(a) FROM PerformanceParticipation a " +
            "WHERE a.user.id = :userId " +
            "AND a.performance.status = :status " +
            "AND (a.performance.performanceDate < :today " +
            "     OR (a.performance.performanceDate = :today AND a.performance.startTime < :now))")
    long countParticipatedByUserId(
            @Param("userId") Long userId,
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now
    );
}
