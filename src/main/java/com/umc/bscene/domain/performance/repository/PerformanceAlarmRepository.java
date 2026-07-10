package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceAlarmRepository extends JpaRepository<PerformanceAlarm, Long> {

    // 사용자와 공연 사이에 알림 신청이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 알림 신청을 삭제
    void deleteByPerformance_IdAndUser_Id(Long performanceId, Long userId);
}
