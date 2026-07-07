package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    List<Performance> findByBand_IdAndStatusOrderByPerformanceDateAsc(Long bandId, PerformanceStatus status);

    Long countByBand_IdAndStatus(Long bandId, PerformanceStatus status);
}
