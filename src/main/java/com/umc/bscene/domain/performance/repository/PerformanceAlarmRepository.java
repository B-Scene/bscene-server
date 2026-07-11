package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceAlarm;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface PerformanceAlarmRepository extends JpaRepository<PerformanceAlarm, Long> {

    // 사용자와 공연 사이에 알림 신청이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 알림 신청을 삭제
    void deleteByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 마이페이지 참여 공연 수 : 알림 신청한 공연 중 시작 시각이 지난 공연 수 (삭제된 공연 제외)
    @Query("SELECT COUNT(a) FROM PerformanceAlarm a " +
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
