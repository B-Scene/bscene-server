package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
}
