package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceInterestRepository extends JpaRepository<PerformanceInterest, Long> {

    // 사용자와 공연 사이에 관심 등록이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 공연에 대한 관심 등록 수 집계
    long countByPerformance_Id(Long performanceId);

    // 사용자와 공연 사이의 관심 등록을 삭제
    void deleteByPerformance_IdAndUser_Id(Long performanceId, Long userId);
}
