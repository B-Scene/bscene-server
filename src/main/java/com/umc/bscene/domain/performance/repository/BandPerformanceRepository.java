package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.BandPerformance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BandPerformanceRepository extends JpaRepository<BandPerformance, Long> {

    List<BandPerformance> findByBand_IdAndPerformance_StatusOrderByPerformance_PerformanceDateAsc(
            Long bandId, PerformanceStatus status
    );

    boolean existsByBand_IdAndPerformance_Id(Long bandId, Long performanceId);

    List<BandPerformance> findByPerformance_Id(Long performanceId);
}
