package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceInterest;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceInterestRepository extends JpaRepository<PerformanceInterest, Long> {

    // 사용자와 공연 사이에 관심 등록이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 공연에 대한 관심 등록 수 집계
    long countByPerformance_Id(Long performanceId);

    // 사용자가 관심 등록한 공연 수 집계 (삭제된 공연 제외)
    @Query("SELECT COUNT(pi) FROM PerformanceInterest pi " +
            "WHERE pi.user.id = :userId AND pi.performance.status = :status")
    long countInterestedByUserId(
            @Param("userId") Long userId,
            @Param("status") PerformanceStatus status
    );

    // 공연 목록에서 사용자가 관심 등록한 공연 id만 일괄 조회 (관심 여부 표시용)
    @Query("SELECT pi.performance.id FROM PerformanceInterest pi " +
            "WHERE pi.user.id = :userId AND pi.performance.id IN :performanceIds")
    List<Long> findInterestedPerformanceIds(
            @Param("userId") Long userId,
            @Param("performanceIds") List<Long> performanceIds
    );

    // 사용자와 공연 사이의 관심 등록을 삭제
    void deleteByPerformance_IdAndUser_Id(Long performanceId, Long userId);
}
