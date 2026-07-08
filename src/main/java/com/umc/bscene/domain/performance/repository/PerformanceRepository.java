package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    List<Performance> findByBand_IdAndStatusOrderByPerformanceDateAsc(Long bandId, PerformanceStatus status);

    // 후보 밴드 중 최근 30일 내 공연 이력이 있는 밴드 id만 추려서 N+1 조회를 피함
    @Query("SELECT DISTINCT p.band.id FROM Performance p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status AND p.performanceDate >= :since")
    List<Long> findBandIdsWithRecentPerformance(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("since") LocalDate since
    );

    Long countByBand_IdAndStatus(Long bandId, PerformanceStatus status);
}
